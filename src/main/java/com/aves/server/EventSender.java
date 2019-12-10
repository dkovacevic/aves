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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class EventSender {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static ObjectMapper mapper = new ObjectMapper();

    public static String sendEvent(Event event, UUID to, DBI jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
        ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

        String strEvent = mapper.writeValueAsString(event);

        for (String clientId : clientsDAO.getClients(to)) {
            // persist to Notification stream
            notificationsDAO.insert(UUID.randomUUID(), clientId, to, strEvent);

            //Send event via Socket
            boolean send = ServerEndpoint.send(clientId, event);
            Logger.debug("Websocket: message (%s) to user: %s, client: %s. Sent: %s",
                    event.id,
                    to,
                    clientId,
                    send);

            Logger.debug("sendEvent: to: %s:%s %s", to, clientId, strEvent);
        }

        return strEvent;
    }

    public static String sendEvent(Event event, UUID to, String clientId, DBI jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);

        // Persist event into Notification stream
        String strEvent = mapper.writeValueAsString(event);
        notificationsDAO.insert(UUID.randomUUID(), clientId, to, strEvent);

        // Send event via Socket
        boolean send = ServerEndpoint.send(clientId, event);
        Logger.debug("Websocket: message (%s) to user: %s, client: %s. Sent: %s",
                event.id,
                to,
                clientId,
                send);

        Logger.debug("sendEvent: to: %s:%s %s", to, clientId, strEvent);

        return strEvent;
    }

    public static Event conversationCreateEvent(UUID from, Conversation conv) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.convId = conv.id;
        payload.from = from;
        payload.type = "conversation.create";
        payload.time = formatter.format(new Date());
        payload.data = new Payload.Data();
        payload.data.id = conv.id;
        payload.data.creator = conv.creator;
        payload.data.name = conv.name;
        payload.data.type = conv.type;
        payload.data.members = conv.members;

        event.payload = new ArrayList<>();
        event.payload.add(payload);

        if (conv.members == null)
            Logger.error("conversationCreateEvent: conv.members is NULL");

        return event;
    }

    public static Event conversationOtrMessageAddEvent(UUID convId, UUID from, Payload.Data data) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.convId = convId;
        payload.from = from;
        payload.type = "conversation.otr-message-add";
        payload.time = formatter.format(new Date());
        payload.data = data;

        event.payload = new ArrayList<>();
        event.payload.add(payload);
        
        return event;
    }
}
