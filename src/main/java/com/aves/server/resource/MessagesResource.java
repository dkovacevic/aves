package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.model.ClientMismatch;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.NewOtrMessage;
import io.swagger.annotations.*;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
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
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @PathParam("convId") UUID convId,
                         @ApiParam @Valid NewOtrMessage otrMessage) {

        try {
            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            ClientMismatch clientMismatch = new ClientMismatch();

            List<UUID> participants = participantsDAO.get(convId);
            for (UUID participantId : participants) {
                List<String> clientIds = clientsDAO.getClients(participantId);

                for (String clientId : clientIds) {
                    if (!otrMessage.recipients.contains(participantId, clientId))
                        clientMismatch.missing.add(participantId, clientId);
                }
            }

            if (!clientMismatch.missing.isEmpty())
                return Response.
                        ok(clientMismatch).
                        status(412).
                        build();

            //todo Send event via Socket

            return Response.
                    ok(clientMismatch).
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
