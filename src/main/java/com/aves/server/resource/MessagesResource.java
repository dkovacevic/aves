package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.ParticipantsDAO;
import com.aves.server.DAO.PushTokensDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.PushToken;
import com.aves.server.model.otr.*;
import com.aves.server.notifications.CompositeNotificationService;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.apache.http.annotation.Obsolete;
import org.jdbi.v3.core.Jdbi;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.aves.server.EventSender.conversationOtrMessageAddEvent;
import static com.aves.server.EventSender.sendEvent;
import static com.aves.server.tools.Util.time;
import static com.aves.server.tools.Util.toBigInteger;

@Api
@Path("/conversations/{convId}/otr/messages")
@Produces(MediaType.APPLICATION_JSON)
public class MessagesResource {
    private final Jdbi jdbi;
    private final ParticipantsDAO participantsDAO;
    private final ClientsDAO clientsDAO;
    private final PushTokensDAO pushTokensDAO;

    public MessagesResource(Jdbi jdbi) {
        participantsDAO = jdbi.onDemand(ParticipantsDAO.class);
        clientsDAO = jdbi.onDemand(ClientsDAO.class);
        pushTokensDAO = jdbi.onDemand(PushTokensDAO.class);
        this.jdbi = jdbi;
    }

    @POST
    @Authorization("Bearer")
    @Consumes("application/x-protobuf")
    public Response postProtobuf(@Context ContainerRequestContext context,
                                 @PathParam("convId") UUID convId,
                                 @QueryParam("report_missing") UUID reportMissing,
                                 @QueryParam("ignore_missing") boolean ignoreMissing,
                                 Otr.NewOtrMessage payload) {

        NewOtrMessage otrMessage = new NewOtrMessage();
        otrMessage.recipients = new Recipients();
        otrMessage.sender = toBigInteger(payload.getSender().getClient()).toString(16);

        for (Otr.UserEntry entry : payload.getRecipientsList()) {
            UUID userId = Util.getGuidFromByteArray(entry.getUser().getUuid().toByteArray());
            for (Otr.ClientEntry clientEntry : entry.getClientsList()) {
                String clientId = toBigInteger(clientEntry.getClient().getClient()).toString(16);
                String cipher = Base64.getEncoder().encodeToString(clientEntry.getText().toByteArray());

                otrMessage.recipients.add(userId, clientId, cipher);
            }
        }

        return postJson(context, convId, reportMissing, ignoreMissing, otrMessage);
    }

    @POST
    @ApiOperation(value = "Post new Otr Message")
    @Authorization("Bearer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postJson(@Context ContainerRequestContext context,
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
                List<PushToken> tokens = pushTokensDAO.getPushTokens(participantId);
                Map<String, PushToken> clientPushToken = new LinkedHashMap<>();
                for (PushToken token : tokens) {
                    clientPushToken.put(token.client, token);
                }

                for (String clientId : clientCipher.keySet()) {
                    if (!Objects.equals(sender, clientId)) {
                        OtrEvent data = new OtrEvent();
                        data.sender = sender;
                        data.recipient = clientId;
                        data.text = clientCipher.get(clientId);

                        Event event = conversationOtrMessageAddEvent(convId, userId, data);

                        // Send Event
                        sendEvent(event, participantId, clientId, jdbi);

                        // Send Notification to phone clients
                        PushToken pushToken = clientPushToken.get(clientId);
                        if (pushToken != null) {
                            CompositeNotificationService.getInstance().send(
                                    userId.toString(),
                                    event.id.toString(),
                                    pushToken
                            );
                        }
                    }
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

    @GET
    @ApiOperation(value = "Dummy")
    @Authorization("Bearer")
    @Obsolete
    public Response get(@Context ContainerRequestContext context,
                        @PathParam("convId") UUID convId) {

        UUID userId = (UUID) context.getProperty("zuid");

        List<String> clients = clientsDAO.getClients(userId);
        NewOtrMessage message = new NewOtrMessage();
        message.sender = clients.get(0);
        message.recipients = new Recipients();

        return Response.
                ok(message).
                build();
    }

    private ClientMismatch checkMissing(UUID reportMissing, String sender, Recipients recipients, List<UUID> participants) {
        ClientMismatch clientMismatch = new ClientMismatch();
        clientMismatch.time = time();

        for (UUID participantId : participants) {
            if (reportMissing != null && !Objects.equals(reportMissing, participantId))
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
