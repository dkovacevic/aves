package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.Payload;
import com.aves.server.model.otr.ClientMismatch;
import com.aves.server.model.otr.NewOtrMessage;
import com.aves.server.tools.Logger;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.aves.server.EventSender.conversationOtrMessageAddEvent;
import static com.aves.server.EventSender.sendEvent;

@Api
@Path("/conversations/{convId}/otr/messages")
@Produces(MediaType.APPLICATION_JSON)
public class MessagesResource {
    private final DBI jdbi;
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public MessagesResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Post new Otr Message")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @PathParam("convId") UUID convId,
                         @QueryParam("report_missing") UUID missing,
                         @ApiParam @Valid NewOtrMessage otrMessage) {

        try {
            UUID userId = (UUID) context.getProperty("zuid");

            ParticipantsDAO participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            UUID challenge = clientsDAO.getUserId(otrMessage.sender);
            if (!Objects.equals(challenge, userId)) {
                Logger.warning("%s -> Unknown sender: %s %s", userId, challenge, otrMessage.sender);
                return Response.
                        status(403).
                        build();
            }

            ClientMismatch clientMismatch = new ClientMismatch();
            clientMismatch.time = formatter.format(new Date());

            List<UUID> participants = participantsDAO.getUsers(convId);
            for (UUID participantId : participants) {
                List<String> clientIds = clientsDAO.getClients(participantId);

                for (String clientId : clientIds) {
                    if (clientId.equals(otrMessage.sender))
                        continue;

                    if (!otrMessage.recipients.contains(participantId, clientId)) {
                        Logger.info("Missing: %s %s", participantId, clientId);
                        clientMismatch.missing.add(participantId, clientId);
                    }
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
                    if (clientId.equals(otrMessage.sender))
                        continue;

                    Payload.Data data = new Payload.Data();
                    data.sender = otrMessage.sender;
                    data.recipient = clientId;
                    data.text = otrMessage.recipients.get(participantId, clientId);

                    Event event = conversationOtrMessageAddEvent(convId, userId, data);

                    // Send Event
                    sendEvent(event, participantId, clientId, jdbi);
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
