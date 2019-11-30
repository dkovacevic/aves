package com.aves.server.websocket;

import com.aves.server.Aves;
import com.aves.server.DAO.ClientsDAO;
import com.aves.server.Logger;
import com.aves.server.Util;
import com.aves.server.model.Event;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.aves.server.Aves.jdbi;

@Metered
@Timed
public class ServerEndpoint extends Endpoint {
    private final static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();// ClientID, Session,

    public static boolean send(String clientId, Event event) {
        Session session = sessions.get(clientId);
        if (session != null) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendObject(event);
                return true;
            } else {
                sessions.remove(clientId);
                close(session);
            }
        }
        return false;
    }

    private static void close(Session session) {
        try {
            session.close();
        } catch (IOException e1) {
            Logger.error("session.close(): %s", e1);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
            String clientId = Util.getQueryParam(session.getQueryString(), "client");
            String token = Util.getQueryParam(session.getQueryString(), "access_token");

            if (token == null || clientId == null) {
                Logger.warning("Session %s missing token or clientId", session.getId());
                return;
            }

            String subject = Jwts.parser()
                    .setSigningKey(Aves.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);

            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);
            UUID challenge = clientsDAO.getUserId(clientId);
            if (!userId.equals(challenge)) {
                Logger.warning("Session: %s, client: %s. Unknown clientId", session.getId(), clientId);
                return;
            }

            session.addMessageHandler(new PingMessageHandler(session));
            session.getUserProperties().put("client", clientId);

            Logger.info("Session: %s connected. client: %s", session.getId(), clientId);
            sessions.put(clientId, session);
        } catch (ExpiredJwtException e) {
            Logger.warning("onOpen: %s", e);
        } catch (Exception e) {
            Logger.error("onOpen: %s", e);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        Object client = session.getUserProperties().get("client");
        Logger.info("Session: %s closed. client: %s, reason: %s", session.getId(), client, closeReason.getReasonPhrase());
    }
}