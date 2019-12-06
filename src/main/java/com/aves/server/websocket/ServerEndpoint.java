package com.aves.server.websocket;

import com.aves.server.Aves;
import com.aves.server.DAO.ClientsDAO;
import com.aves.server.model.Event;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Objects;
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
            Logger.debug("Closing session: %s", session.getId());
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

            if (token == null) {
                Logger.warning("Session %s, missing access token: %s", session.getId(), session.getQueryString());
                return;
            }

            String subject = Jwts.parser()
                    .setSigningKey(Aves.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);

            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            session.addMessageHandler(new PingMessageHandler(session));

            for (String client : clientsDAO.getClients(userId)) {
                //session.getUserProperties().put("client", client);
                sessions.put(client, session);

                if (Objects.equals(client, clientId)) {
                    break;
                }
            }

            Logger.debug("Session: %s connected. zuid: %s", session.getId(), session.getUserProperties().get("zuid"));
        } catch (ExpiredJwtException e) {
            Logger.warning("onOpen: %s", e);
        } catch (Exception e) {
            Logger.error("onOpen: %s", e);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        Object client = session.getUserProperties().get("client");
        Logger.debug("Session: %s closed. client: %s, reason: %s", session.getId(), client, closeReason);
    }
}