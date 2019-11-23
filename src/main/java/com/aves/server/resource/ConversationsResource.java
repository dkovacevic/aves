package com.aves.server.resource;

import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.Conversation;
import com.aves.server.model.ErrorMessage;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.*;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    public Response create(@ApiParam(hidden = true) @NotNull @CookieParam("Authorization") String cookie,
                           @ApiParam @Valid Conversation conversation) {

        try {
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);

            String token = Jwts.parser()
                    .setSigningKey(Server.getKey())
                    .parseClaimsJws(cookie)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(token);
            UUID convId = UUID.randomUUID();

            conversationsDAO.insert(convId, conversation.name, userId);
            for (UUID participantId : conversation.users) {
                participantsDAO.insert(convId, participantId);
            }
            participantsDAO.insert(convId, userId);

            return Response.
                    accepted().
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
