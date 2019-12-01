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
import java.util.List;
import java.util.UUID;

import static com.aves.server.EventSender.conversationCreateEvent;
import static com.aves.server.EventSender.sendEvent;

@Api
@Path("/conversations")
@Produces(MediaType.APPLICATION_JSON)
public class ConversationsResource {

    private final DBI jdbi;

    public ConversationsResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Create a new conversation")
    @Authorization("Bearer")
    public Response create(@Context ContainerRequestContext context, @ApiParam @Valid NewConversation conv) {

        try {
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

            UUID userId = (UUID) context.getProperty("zuid");
            UUID convId = UUID.randomUUID();

            // persist conv
            conversationsDAO.insert(convId, conv.name, userId, Enums.Conversation.REGULAR.ordinal());

            participantsDAO.insert(convId, userId);
            for (UUID participantId : conv.users) {
                participantsDAO.insert(convId, participantId);
            }

            Conversation conversation = conversationsDAO.get(convId);

            // build result
            List<UUID> others = participantsDAO.getUsers(convId);
            for (UUID selfId : others) {
                conversation = buildConversation(conversation, selfId, others);

                // Send event
                Event event = conversationCreateEvent(userId, conversation);
                sendEvent(event, selfId, jdbi);
            }

            Logger.debug("New conversation: %s", convId);

            conversation = buildConversation(conversation, userId, others);

            if (conversation.members == null || conversation.members.self == null)
                Logger.error("conversationCreateEvent: conv.members is NULL");
            
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

    @GET
    @Path("{convId}")
    @Authorization("Bearer")
    @ApiOperation(value = "Get Conversation by conv id")
    public Response get(@Context ContainerRequestContext context, @PathParam("convId") UUID convId) {

        ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
        ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

        UUID userId = (UUID) context.getProperty("zuid");

        UUID challenge = participantsDAO.isParticipant(userId, convId);
        if (challenge == null) {
            return Response.
                    status(404).
                    build();
        }

        Conversation conversation = conversationsDAO.get(convId);
        if (conversation == null) {
            return Response.
                    status(404).
                    build();
        }

        List<UUID> others = participantsDAO.getUsers(convId);

        conversation = buildConversation(conversation, userId, others);

        if (conversation.members == null || conversation.members.self == null)
            Logger.error("conversationCreateEvent: conv.members is NULL");

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

        List<UUID> convIds = participantsDAO.getConversations(userId);

        for (UUID convId : convIds) {
            Conversation conversation = conversationsDAO.get(convId);
            List<UUID> others = participantsDAO.getUsers(convId);
            conversation = buildConversation(conversation, userId, others);

            result.conversations.add(conversation);

            if (conversation.members == null || conversation.members.self == null)
                Logger.error("conversationCreateEvent: conv.members is NULL");
        }

        return Response.
                ok(result).
                build();
    }

    private Conversation buildConversation(Conversation conversation, UUID selfId, List<UUID> others) {
        conversation.members = new Members();
        conversation.members.self.id = selfId;
        for (UUID memberId : others) {
            if (!memberId.equals(selfId)) {
                Member member = new Member();
                member.id = memberId;
                conversation.members.others.add(member);
            }
        }
        return conversation;
    }

    public static class _Result {
        @JsonProperty("has_more")
        public boolean hasMore;
        public List<Conversation> conversations = new ArrayList<>();
    }
}
