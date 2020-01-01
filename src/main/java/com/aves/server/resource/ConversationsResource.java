package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.model.*;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.aves.server.EventSender.*;

@Api
@Path("/conversations")
@Produces(MediaType.APPLICATION_JSON)
public class ConversationsResource {

    private final Jdbi jdbi;

    public ConversationsResource(Jdbi jdbi) {
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

            // persist conv
            Conversation conversation = new Conversation();
            conversation.id = UUID.randomUUID();
            conversation.creator = userId;
            conversation.type = Enums.Conversation.REGULAR.ordinal();
            conversation.name = conv.name;
            conversation.lastEventTime = "1970-01-01T00:00:00.000Z";
            conversation.lastEvent = "0.0";

            conversationsDAO.insert(conversation);

            UUID convId = conversation.id;

            participantsDAO.insert(convId, userId);
            for (UUID participantId : conv.users) {
                participantsDAO.insert(convId, participantId);
            }

            // build result
            List<UUID> others = participantsDAO.getUsers(convId);
            for (UUID selfId : others) {
                buildConversation(conversation, selfId, others);

                // Send event
                Event event = conversationCreateEvent(userId, conversation);
                sendEvent(event, selfId, jdbi);
            }

            buildConversation(conversation, userId, others);

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

        buildConversation(conversation, userId, others);

        return Response.
                ok(conversation).
                build();
    }

    @POST
    @Path("{convId}/members")
    @Authorization("Bearer")
    @ApiOperation(value = "Add participants")
    public Response addMembers(@Context ContainerRequestContext context,
                               @PathParam("convId") UUID convId,
                               Users invite) throws JsonProcessingException {

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

        for (UUID participantId : invite.users) {
            participantsDAO.insert(convId, participantId);
        }

        UserIds userIds = new UserIds();
        userIds.usersIds = invite.users;

        List<UUID> participants = participantsDAO.getUsers(convId);
        for (UUID participant : participants) {
            Event event = memberJoinEvent(userId, convId, userIds);
            sendEvent(event, participant, jdbi);
        }

        return Response.
                ok(memberJoinEvent(userId, convId, userIds)).
                build();
    }

    @DELETE
    @Path("{convId}/members/{member}")
    @Authorization("Bearer")
    @ApiOperation(value = "Remove member from the conversation")
    public Response removeMember(@Context ContainerRequestContext context,
                                 @PathParam("convId") UUID convId,
                                 @PathParam("member") UUID member) throws JsonProcessingException {

        ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
        ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

        UUID userId = (UUID) context.getProperty("zuid");

        UUID challenge = participantsDAO.isParticipant(userId, convId);
        if (challenge == null) {
            return Response.
                    status(403).
                    build();
        }

        Conversation conversation = conversationsDAO.get(convId);
        if (conversation == null) {
            return Response.
                    status(404).
                    build();
        }

        participantsDAO.remove(convId, member);

        UserIds userIds = new UserIds();
        userIds.usersIds = Collections.singletonList(member);

        List<UUID> participants = participantsDAO.getUsers(convId);
        for (UUID participant : participants) {
            Event event = memberLeaveEvent(userId, convId, userIds);
            sendEvent(event, participant, jdbi);
        }
        return Response.
                ok(memberLeaveEvent(userId, convId, userIds)).
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
            buildConversation(conversation, userId, others);

            result.conversations.add(conversation);
        }

        return Response.
                ok(result).
                build();
    }

    private void buildConversation(Conversation conversation, UUID selfId, List<UUID> others) {
        conversation.members = new Members();
        conversation.members.self.id = selfId;
        for (UUID memberId : others) {
            if (!memberId.equals(selfId)) {
                Member member = new Member();
                member.id = memberId;
                conversation.members.others.add(member);
            }
        }
    }

    public static class _Result {
        @JsonProperty("has_more")
        public boolean hasMore;
        public List<Conversation> conversations = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Users {
        @NotNull
        public List<UUID> users;
    }
}
