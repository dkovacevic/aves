package com.aves.server;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.NotificationsDAO;
import com.aves.server.model.Conversation;
import com.aves.server.model.Event;
import com.aves.server.model.Payload;
import com.aves.server.tools.Logger;
import com.aves.server.websocket.ServerEndpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skife.jdbi.v2.DBI;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventSender {
    private static ObjectMapper mapper = new ObjectMapper();

    @Deprecated
    public static void sendEvent(Event event, List<UUID> recipients, DBI jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
        ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

        String notification = mapper.writeValueAsString(event);
        for (UUID participantId : recipients) {
            List<String> clientIds = clientsDAO.getClients(participantId);
            for (String clientId : clientIds) {
                // persist to Notification stream
                notificationsDAO.insert(UUID.randomUUID(), clientId, participantId, notification);

                //Send event via Socket
                boolean send = ServerEndpoint.send(clientId, event);
                Logger.debug("Websocket: message (%s) to user: %s, client: %s. Sent: %s",
                        event.id,
                        participantId,
                        clientId,
                        send);
            }
        }
    }

    public static void sendEvent(Event event, UUID to, DBI jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
        ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

        for (String clientId : clientsDAO.getClients(to)) {
            // persist to Notification stream
            String notification = mapper.writeValueAsString(event);
            notificationsDAO.insert(UUID.randomUUID(), clientId, to, notification);

            //Send event via Socket
            boolean send = ServerEndpoint.send(clientId, event);
            Logger.debug("Websocket: message (%s) to user: %s, client: %s. Sent: %s",
                    event.id,
                    to,
                    clientId,
                    send);
        }
    }

    public static void sendEvent(Event event, UUID recipient, String clientId, DBI jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);

        // Persist event into Notification stream
        String strEvent = mapper.writeValueAsString(event);
        notificationsDAO.insert(UUID.randomUUID(), clientId, recipient, strEvent);

        // Send event via Socket
        boolean send = ServerEndpoint.send(clientId, event);
        Logger.debug("Websocket: message (%s) to user: %s, client: %s. Sent: %s",
                event.id,
                recipient,
                clientId,
                send);
    }

    public static Event conversationCreateEvent(UUID from, Conversation conv) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.convId = conv.id;
        payload.from = from;
        payload.type = "conversation.create";
        payload.time = new Date().toString();
        payload.data = new Payload.Data();
        payload.data.id = conv.id;
        payload.data.creator = conv.creator;
        payload.data.name = conv.name;
        payload.data.type = conv.type;
        payload.data.members = conv.members;

        event.payload = new Payload[]{payload};

        return event;
    }

    public static Event conversationOtrMessageAddEvent(UUID convId, UUID from, Payload.Data data) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.convId = convId;
        payload.from = from;
        payload.type = "conversation.otr-message-add";
        payload.time = new Date().toString();
        payload.data = data;

        event.payload = new Payload[]{payload};
        return event;
    }
}
