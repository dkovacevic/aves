package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.ConversationsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.ClientMismatch;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.NewOtrMessage;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.*;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Api
@Path("/conversations/{convId}/otr/messages")
@Produces(MediaType.APPLICATION_JSON)
public class MessagesResource {
    private final DBI jdbi;

    public MessagesResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Post new Otr Message")
    @ApiResponses(value = {@ApiResponse(code = 403, message = "Wrong email or password")})
    public Response post(@ApiParam(hidden = true) @NotNull @CookieParam("Authorization") String cookie,
                         @PathParam("convId") UUID convId,
                         @ApiParam @Valid NewOtrMessage otrMessage) {

        try {
            ConversationsDAO conversationsDAO = jdbi.onDemand(ConversationsDAO.class);
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            String token = Jwts.parser()
                    .setSigningKey(Server.getKey())
                    .parseClaimsJws(cookie)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(token);

            ClientMismatch clientMismatch = new ClientMismatch();

            List<UUID> participants = participantsDAO.get(convId);
            for (UUID participantId : participants) {
                List<String> clientIds = clientsDAO.get(participantId);

                for (String clientId : clientIds) {
                    if (!otrMessage.recipients.contains(participantId, clientId))
                        clientMismatch.missing.add(participantId, clientId);
                }
            }

            return Response.
                    ok(clientMismatch).
                    status(412).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("MessagesResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
