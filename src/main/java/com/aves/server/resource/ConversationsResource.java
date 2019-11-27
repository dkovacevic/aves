package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.model.Conversation;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Member;
import com.aves.server.model.NewConversation;
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
    public Response create(@Context ContainerRequestContext context,
                           @ApiParam @Valid NewConversation conv) {

        try {
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

            UUID userId = (UUID) context.getProperty("zuid");
            UUID convId = UUID.randomUUID();

            ArrayList<Member> members = new ArrayList<>();

            conversationsDAO.insert(convId, conv.name, userId);
            for (UUID participantId : conv.users) {
                Member member = new Member();
                member.id = participantId;
                members.add(member);

                participantsDAO.insert(convId, participantId);
            }
            participantsDAO.insert(convId, userId);

            Conversation result = new Conversation();
            result.name = conv.name;
            result.id = convId;
            result.creator = userId;
            result.members.others = members;

            Logger.info("New conversation: %s", convId);

            return Response.
                    ok(result).
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
    public Response get(@Context ContainerRequestContext context,
                        @QueryParam("convId") UUID convId) {

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

    public static class _Result {
        @JsonProperty("has_more")
        public boolean hasMore;
        public List<Conversation> conversations;
    }
}
