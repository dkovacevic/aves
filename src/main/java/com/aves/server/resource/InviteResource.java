package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.DAO.UserDAO;
import com.aves.server.model.*;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Picture;
import com.lambdaworks.crypto.SCryptUtil;
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
import java.util.UUID;

import static com.aves.server.EventSender.conversationCreateEvent;
import static com.aves.server.EventSender.sendEvent;
import static com.aves.server.tools.Util.*;

@Api
@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
public class InviteResource {
    private final DBI jdbi;

    public InviteResource(DBI jdbi) {
        this.jdbi = jdbi;
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
            UUID preview = s3UploadFile(profile.getImageData());

            UUID userId = UUID.randomUUID();

            int accent = random(8);

            userDAO.insert(
                    userId,
                    invite.name,
                    invite.firstname,
                    invite.lastname,
                    invite.country,
                    invite.email,
                    invite.phone,
                    accent,
                    preview,
                    preview,
                    hash);

            // create new conv
            UUID convId = UUID.randomUUID();
            conversationsDAO.insert(convId, null, inviterId, Enums.Conversation.ONE2ONE.ordinal());
            participantsDAO.insert(convId, inviterId);
            participantsDAO.insert(convId, userId);

            // send to inviter
            Conversation conversation = conversationsDAO.get(convId);
            conversation.members.self.id = inviterId;
            Member member = new Member();
            member.id = userId;
            conversation.members.others.add(member);

            Event event = conversationCreateEvent(inviterId, conversation);
            sendEvent(event, inviterId, jdbi);

            // send to new user
            conversation = conversationsDAO.get(convId);
            conversation.members.self.id = userId;
            member = new Member();
            member.id = inviterId;
            conversation.members.others.add(member);

            event = conversationCreateEvent(inviterId, conversation);
            sendEvent(event, userId, jdbi);

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

    public static class _InviteResult {
        public _Invitee user;
        public Conversation conversation;
    }

    public static class _Invitee extends NewUser {
        public UUID id;
        public String password;
    }
}
