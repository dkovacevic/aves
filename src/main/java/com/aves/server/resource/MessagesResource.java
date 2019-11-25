package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.Logger;
import com.aves.server.model.*;
import com.aves.server.websocket.WebSocket;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
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
import java.util.Date;
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
            UUID userId = (UUID) context.getProperty("zuid");

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

            for (UUID participantId : participants) {
                List<String> clientIds = clientsDAO.getClients(participantId);
                for (String clientId : clientIds) {

                    //Send event via Socket
                    Payload payload = new Payload();
                    payload.convId = convId;
                    payload.from = userId;
                    payload.type = "conversation.otr-message-add";
                    payload.time = new Date().toString();
                    payload.data = new Payload.Data();
                    payload.data.sender = otrMessage.sender;
                    payload.data.text = otrMessage.recipients.get(participantId, clientId);

                    Message message = new Message();
                    message.id = UUID.randomUUID();
                    message.payload = new Payload[]{payload};

                    WebSocket.send(clientId, message);        //todo use execution service to send via socket
                }
            }

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
