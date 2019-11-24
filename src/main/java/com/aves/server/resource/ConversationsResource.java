package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.Conversation;
import com.aves.server.model.Member;
import com.aves.server.model.NewConversation;
import com.aves.server.model.ErrorMessage;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.*;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
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
    @ApiResponses(value = {@ApiResponse(code = 403, message = "Wrong email or password")})
    public Response create(@ApiParam(hidden = true) @NotNull @HeaderParam("Authorization") String cookie,
                           @ApiParam @Valid NewConversation conv) {

        try {
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

            String[] split = cookie.split(" ");
            String token = split[1];

            String subject = Jwts.parser()
                    .setSigningKey(Server.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);
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
