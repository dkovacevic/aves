package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.Payload;
import com.aves.server.model.otr.ClientCipher;
import com.aves.server.model.otr.ClientMismatch;
import com.aves.server.model.otr.NewOtrMessage;
import com.aves.server.model.otr.Recipients;
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
    private final ParticipantsDAO participantsDAO;
    private final ClientsDAO clientsDAO;

    public MessagesResource(DBI jdbi) {
        participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
        clientsDAO = jdbi.onDemand(ClientsDAO.class);
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Post new Otr Message")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @PathParam("convId") UUID convId,
                         @QueryParam("report_missing") UUID reportMissing,
                         @QueryParam("ignore_missing") boolean ignoreMissing,
                         @ApiParam @Valid NewOtrMessage otrMessage) {

        try {
            UUID userId = (UUID) context.getProperty("zuid");

            String sender = otrMessage.sender;
            UUID challenge = clientsDAO.getUserId(sender);
            if (!Objects.equals(challenge, userId)) {
                Logger.warning("%s -> Unknown sender: %s %s", userId, challenge, sender);
                return Response.
                        status(403).
                        build();
            }
            Recipients recipients = otrMessage.recipients;
            List<UUID> participants = participantsDAO.getUsers(convId);

            ClientMismatch clientMismatch = checkMissing(reportMissing, sender, recipients, participants);

            if (!ignoreMissing && !clientMismatch.missing.isEmpty())
                return Response.
                        ok(clientMismatch).
                        status(412).
                        build();

            for (UUID participantId : recipients.keySet()) {
                ClientCipher clientCipher = recipients.get(participantId);
                for (String clientId : clientCipher.keySet()) {
                    Payload.Data data = new Payload.Data();
                    data.sender = sender;
                    data.recipient = clientId;
                    data.text = clientCipher.get(clientId);

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

    private ClientMismatch checkMissing(UUID ignore, String sender, Recipients recipients, List<UUID> participants) {
        ClientMismatch clientMismatch = new ClientMismatch();
        clientMismatch.time = formatter.format(new Date());

        for (UUID participantId : participants) {
            if (Objects.equals(ignore, participantId))
                continue;

            for (String clientId : clientsDAO.getClients(participantId)) {
                if (Objects.equals(clientId, sender))
                    continue;

                if (!recipients.contains(participantId, clientId)) {
                    clientMismatch.missing.add(participantId, clientId);
                }
            }
        }
        return clientMismatch;
    }
}
