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
import java.nio.ByteBuffer;
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
            }
        }
        return false;
    }

    public static void ping() {
        for (Session session : sessions.values()) {
            Object client = session.getUserProperties().get("client");
            Object zuid = session.getUserProperties().get("zuid");

            if (session.isOpen()) {
                try {
                    Logger.debug("Sending ping: session: %s,  zuid: %s, client: %s", session.getId(), zuid, client);
                    session.getBasicRemote().sendPing(ByteBuffer.wrap("ping from Aves".getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (client != null)
                    sessions.remove(client);
            }
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
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

            session.getUserProperties().put("zuid", userId);

            session.addMessageHandler(new PingMessageHandler(session));

            String clientId = Util.getQueryParam(session.getQueryString(), "client");
            if (clientId != null) {
                sessions.put(clientId, session);
                session.getUserProperties().put("client", clientId);
            } else {
                // use this session to channel events for ALL clients
                ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);
                for (String client : clientsDAO.getClients(userId)) {
                    sessions.put(client, session);
                }
            }

            Logger.debug("Session: %s connected. zuid: %s, client: %s", session.getId(), userId, clientId);
        } catch (ExpiredJwtException e) {
            Logger.warning("onOpen: %s", e);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("onOpen: %s", e);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        Object client = session.getUserProperties().get("client");
        Object userId = session.getUserProperties().get("zuid");

        Logger.debug("Session: %s closed. zuid:%s, client: %s, %s", session.getId(), userId, client, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        Object client = session.getUserProperties().get("client");
        Object userId = session.getUserProperties().get("zuid");

        Logger.debug("Session: %s failed. zuid:%s, client: %s, %s", session.getId(), userId, client, thr.getMessage());
    }
}