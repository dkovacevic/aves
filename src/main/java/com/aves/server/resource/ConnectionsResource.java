package com.aves.server.resource;

import com.aves.server.DAO.ConnectionsDAO;
import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.model.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.aves.server.EventSender.*;
import static com.aves.server.tools.Util.time;

@Api
@Path("/connections")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionsResource {
    private final ConnectionsDAO connectionsDAO;
    private final ConversationsDAO conversationsDAO;
    private final ParticipantsDAO participantsDAO;
    private final Jdbi jdbi;

    public ConnectionsResource(Jdbi jdbi) {
        connectionsDAO = jdbi.onDemand(ConnectionsDAO.class);
        conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
        participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
        this.jdbi = jdbi;
    }

    @GET
    @ApiOperation(value = "Get user's connections")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context) {
        UserConnectionList result = new UserConnectionList();

        UUID userId = (UUID) context.getProperty("zuid");

        List<UUID> connections = connectionsDAO.getConnections(userId);
        for (UUID to : connections) {
            Connection connection = new Connection();
            connection.time = time();
            connection.from = userId;
            connection.to = to;

            result.connections.add(connection);
        }
        return Response.
                ok(result).
                build();
    }

    @POST
    @ApiOperation(value = "Connect to user")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @Valid ConnectionRequest request) throws JsonProcessingException {

        UUID userId = (UUID) context.getProperty("zuid");

        connectionsDAO.insert(userId, request.user);
        connectionsDAO.insert(request.user, userId);

        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID();
        conversation.creator = userId;
        conversation.type = Enums.Conversation.ONE2ONE.ordinal();

        conversationsDAO.insert(conversation);

        UUID convId = conversation.id;

        participantsDAO.insert(convId, userId);
        participantsDAO.insert(convId, request.user);

        // Send Conversation event
        sendConversationEvent(userId, request.user, convId);
        sendConversationEvent(request.user, userId, convId);

        // Send Connection event
        Connection connection = sendConnectionEvent(request.user, userId, convId);
        sendConnectionEvent(userId, request.user, convId);

        return Response.
                ok(connection).
                status(201).
                build();
    }

    private void sendConversationEvent(UUID other, UUID self, UUID convId) throws JsonProcessingException {
        Conversation conversation = conversationsDAO.get(convId);
        conversation.members.self.id = self;
        Member member2 = new Member();
        member2.id = other;
        conversation.members.others.add(member2);
        Event event = conversationCreateEvent(self, conversation);
        sendEvent(event, self, jdbi);
    }

    private Connection sendConnectionEvent(UUID from, UUID userId, UUID convId) throws JsonProcessingException {
        Connection connection = new Connection();
        connection.from = from;
        connection.to = userId;
        connection.time = time();
        connection.conversation = convId;
        connection.status = "accepted";
        Event event = connectionEvent(connection);
        sendEvent(event, from, jdbi);
        return connection;
    }

    static class UserConnectionList {
        @JsonProperty("has_more")
        public boolean more;
        @JsonProperty
        public ArrayList<Connection> connections = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ConnectionRequest {
        @NotNull
        public UUID user;
        public String name;
    }
}
