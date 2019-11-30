package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.DAO.UserDAO;
import com.aves.server.model.*;
import com.aves.server.tools.ImageProcessor;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Picture;
import com.lambdaworks.crypto.SCryptUtil;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.aves.server.EventSender.conversationCreateEvent;
import static com.aves.server.EventSender.sendEvent;
import static com.aves.server.tools.Util.next;
import static com.aves.server.tools.Util.toByteArray;

@Api
@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
public class InviteResource {
    private final DBI jdbi;
    private final MinioClient minioClient;
    private final String BUCKET_NAME = "aves-bucket";

    public InviteResource(DBI jdbi) throws InvalidPortException, InvalidEndpointException {
        this.jdbi = jdbi;
        this.minioClient = new MinioClient("http://play.min.io",
                "Q3AM3UQ867SPQQA43P2F",
                "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
    }

    @POST
    @ApiOperation(value = "Invite new user")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @ApiParam @Valid Invite invite) {
        try {
            UUID inviterId = (UUID) context.getProperty("zuid");

            UserDAO userDAO = jdbi.onDemand(UserDAO.class);
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

            String password = next(8);
            String hash = SCryptUtil.scrypt(password, 16384, 8, 1);

            Picture profile = getProfilePicture();
            UUID preview = uploadToS3(profile);

            UUID userId = UUID.randomUUID();
            userDAO.insert(
                    userId,
                    invite.name,
                    invite.firstname,
                    invite.lastname,
                    invite.country,
                    invite.email,
                    invite.phone,
                    new Random().nextInt(8),
                    preview,
                    preview,
                    hash);

            // create new conv
            UUID convId = UUID.randomUUID();
            conversationsDAO.insert(convId, null, inviterId, ConversationsResource.ConversationType.ONE2ONE.ordinal());
            participantsDAO.insert(convId, inviterId);
            participantsDAO.insert(convId, userId);

            Conversation conversation = conversationsDAO.get(convId);
            conversation.members.self.id = userId;

            List<UUID> others = participantsDAO.getUsers(convId);
            conversation.members.others = new ArrayList<>();
            for (UUID participant : others) {
                if (!participant.equals(inviterId)) {
                    Member member = new Member();
                    member.id = participant;
                    conversation.members.others.add(member);
                }
            }

            // Send new event to all participants
            Event event = conversationCreateEvent(inviterId, conversation);
            sendEvent(event, others, jdbi);

            _InviteResult result = new _InviteResult();
            result.user = new _Invitee();
            result.user.id = userId;
            result.user.firstname = invite.firstname;
            result.user.lastname = invite.lastname;
            result.user.name = invite.name;
            result.user.phone = invite.phone;
            result.user.email = invite.email;
            result.user.country = invite.country;
            result.user.password = password;
            result.conversation = conversation;

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("InviteResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    private Picture getProfilePicture() throws Exception {
        String filename = String.format("profiles/%d.png", new Random().nextInt(8));
        InputStream is = InviteResource.class.getClassLoader().getResourceAsStream(filename);
        byte[] image = toByteArray(is);
        return ImageProcessor.getMediumImage(new Picture(image));
    }

    private UUID uploadToS3(Picture complete) throws Exception {
        UUID key = UUID.randomUUID();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(complete.getImageData())) {
            minioClient.putObject(
                    BUCKET_NAME,
                    key.toString(),
                    bais,
                    (long) complete.getSize(),
                    null,
                    null,
                    "application/octet-stream");
        }
        return key;
    }

    public static class _InviteResult {
        public _Invitee user;
        public Conversation conversation;
    }

    public static class _Invitee extends NewUser {
        public UUID id;
        public String password;
    }
}
