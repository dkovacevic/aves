package com.aves.server.resource;

import com.aves.server.DAO.ConnectionsDAO;
import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.DAO.UserDAO;
import com.aves.server.model.*;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Picture;
import com.aves.server.tools.Util;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import static com.aves.server.EventSender.*;
import static com.aves.server.tools.Util.*;

@Api
@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
public class InviteResource {
    private final ConversationsDAO conversationsDAO;
    private final ParticipantsDAO participantsDAO;
    private final ConnectionsDAO connectionsDAO;
    private final DBI jdbi;
    private final UserDAO userDAO;

    public InviteResource(DBI jdbi) {
        userDAO = jdbi.onDemand(UserDAO.class);
        conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
        participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
        connectionsDAO = jdbi.onDemand(ConnectionsDAO.class);
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Invite new user")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @ApiParam @Valid Invite invite) {
        try {
            UUID inviterId = (UUID) context.getProperty("zuid");

            String password = next(8);
            String hash = SCryptUtil.scrypt(password, 16384, 8, 1);

            Picture profile = getProfilePicture();
            String preview = s3UploadFile(profile.getImageData());

            UUID userId = UUID.randomUUID();

            // Save new User
            userDAO.insert(
                    userId,
                    invite.name,
                    invite.firstname,
                    invite.lastname,
                    invite.country,
                    invite.email.toLowerCase().trim(),
                    invite.phone,
                    random(8),
                    preview,
                    preview,
                    hash);

            User user = userDAO.getUser(userId);

            // Create self conv for this user
            createSelfConv(userId);

            // Create new conv
            UUID convId = UUID.randomUUID();
            conversationsDAO.insert(convId, null, inviterId, Enums.Conversation.ONE2ONE.ordinal());
            participantsDAO.insert(convId, inviterId);
            participantsDAO.insert(convId, userId);

            // Save new Connection
            connectionsDAO.insert(inviterId, userId);
            connectionsDAO.insert(userId, inviterId);

            // Send Conversation event
            sendConversationEvent(userId, inviterId, convId);
            sendConversationEvent(inviterId, userId, convId);

            // Send Connection event
            sendConnectionEvent(inviterId, userId, convId);
            sendConnectionEvent(userId, inviterId, convId);

            // Send User Update event to Inviter
            sendEvent(userUpdateEvent(user), inviterId, jdbi);

            Conversation conversation = conversationsDAO.get(convId);

            _InviteResult result = new _InviteResult();
            result.user = user;
            result.conversation = conversation;

            String template = getEmailTemplate();
            String body = template.replace("[USER]", invite.name)
                    .replace("[EMAIL]", user.email)
                    .replace("[PASSWORD]", password);

            Util.sendEmail("Your New Account", body, "aves@wire.com", user.email);

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

    private void sendConversationEvent(UUID inviterId, UUID userId, UUID convId) throws JsonProcessingException {
        Conversation conversation = conversationsDAO.get(convId);
        Member member2 = new Member();
        member2.id = inviterId;
        conversation.members.self.id = userId;
        conversation.members.others.add(member2);
        Event event = conversationCreateEvent(inviterId, conversation);
        sendEvent(event, userId, jdbi);
    }

    private void sendConnectionEvent(UUID from, UUID to, UUID convId) throws JsonProcessingException {
        // Send connection event
        Connection connection = new Connection();
        connection.from = from;
        connection.to = to;
        connection.time = time();
        connection.conversation = convId;
        sendEvent(connectionEvent(connection), from, jdbi);
        sendEvent(connectionEvent(connection), to, jdbi);
    }

    private void createSelfConv(UUID userId) throws JsonProcessingException {
        // create new conv
        UUID convId = UUID.randomUUID();
        conversationsDAO.insert(convId, null, userId, Enums.Conversation.SELF.ordinal());
        participantsDAO.insert(convId, userId);

        Conversation conversation = conversationsDAO.get(convId);
        conversation.members.self.id = userId;

        Event event = conversationCreateEvent(userId, conversation);
        sendEvent(event, userId, jdbi);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class _InviteResult {
        public User user;
        public Conversation conversation;
    }
}
