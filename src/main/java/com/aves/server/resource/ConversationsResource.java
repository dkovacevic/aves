package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.model.*;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.aves.server.EventSender.conversationCreateEvent;
import static com.aves.server.EventSender.sendEvent;

@Api
@Path("/conversations")
@Produces(MediaType.APPLICATION_JSON)
public class ConversationsResource {
    @POST
    @ApiOperation(value = "Create a new conversation")
    @Authorization("Bearer")
    public Response create(@Context ContainerRequestContext context,
                           @ApiParam @Valid NewConversation conv) {

        try {
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

            UUID userId = (UUID) context.getProperty("zuid");
            UUID convId = UUID.randomUUID();

            // persist conv
            conversationsDAO.insert(convId, conv.name, userId, ConversationType.REGULAR.ordinal());

            participantsDAO.insert(convId, userId);
            for (UUID participantId : conv.users) {
                participantsDAO.insert(convId, participantId);
            }

            // build result
            Conversation conversation = conversationsDAO.get(convId);
            conversation.members.self.id = userId;
            List<UUID> others = participantsDAO.getUsers(convId);
            for (UUID participant : others) {
                if (participant.equals(userId)) {
                    continue;
                }
                Member member = new Member();
                member.id = participant;
                conversation.members.others.add(member);
            }

            // Send new event to all participants
            Event event = conversationCreateEvent(userId, conversation);
            sendEvent(event, conv.users, jdbi);

            Logger.info("New conversation: %s", convId);

            return Response.
                    ok(conversation).
                    status(201).
                    header("location", convId.toString()).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ConversationsResource.create : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    private final DBI jdbi;

    public ConversationsResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public enum ConversationType {
        REGULAR,
        SELF,
        ONE2ONE,
        CONNECT
    }

    @GET
    @Path("{convId}")
    @Authorization("Bearer")
    @ApiOperation(value = "Get Conversation by conv id")
    public Response get(@Context ContainerRequestContext context,
                        @PathParam("convId") UUID convId) {

        ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
        ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

        UUID userId = (UUID) context.getProperty("zuid");

        Conversation conversation = conversationsDAO.get(convId);

        if (conversation == null) {
            return Response.
                    status(404).
                    build();
        }

        conversation.members.self.id = userId;

        boolean valid = false;

        List<UUID> others = participantsDAO.getUsers(convId);

        for (UUID participant : others) {
            if (participant.equals(userId)) {
                valid = true;
                continue;
            }

            Member member = new Member();
            member.id = participant;

            conversation.members.others.add(member);
        }

        if (!valid) {
            return Response.
                    status(403).
                    build();
        }

        return Response.
                ok(conversation).
                build();
    }

    @GET
    @Authorization("Bearer")
    @ApiOperation(value = "Get all conversations")
    public Response getAll(@Context ContainerRequestContext context) {

        ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
        ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

        UUID userId = (UUID) context.getProperty("zuid");

        _Result result = new _Result();
        result.conversations = new ArrayList<>();

        List<UUID> convIds = participantsDAO.getConversations(userId);

        for (UUID convId : convIds) {
            Conversation conversation = conversationsDAO.get(convId);
            conversation.members.self.id = userId;

            List<UUID> others = participantsDAO.getUsers(convId);
            for (UUID participant : others) {
                if (participant.equals(userId)) {
                    continue;
                }
                Member member = new Member();
                member.id = participant;

                conversation.members.others.add(member);
            }

            result.conversations.add(conversation);
        }

        return Response.
                ok(result).
                build();
    }

    private Event createEvent(UUID userId, Conversation conv) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.convId = conv.id;
        payload.from = userId;
        payload.type = "conversation.create";
        payload.time = new Date().toString();
        payload.data = new Payload.Data();
        payload.data.id = conv.id;
        payload.data.creator = conv.creator;
        payload.data.name = conv.name;
        payload.data.type = conv.type;
        payload.data.members = conv.members;

        event.payload = new Payload[]{payload};

        return event;
    }

    public static class _Result {
        @JsonProperty("has_more")
        public boolean hasMore;
        public List<Conversation> conversations;
    }
}
