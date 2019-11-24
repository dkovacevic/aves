package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.model.Conversation;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Member;
import com.aves.server.model.NewConversation;
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
import java.util.ArrayList;
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
}
