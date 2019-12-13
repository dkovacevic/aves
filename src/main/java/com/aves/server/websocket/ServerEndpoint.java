package com.aves.server.websocket;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.model.Event;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.jsonwebtoken.ExpiredJwtException;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
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

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
            UUID userId = (UUID) config.getUserProperties().get("zuid");

            session.getUserProperties().put("zuid", userId);

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

            session.addMessageHandler(new PingMessageHandler(session));

            Logger.info("Session: %s connected. zuid: %s, client: %s, size: %d",
                    session.getId(),
                    userId,
                    clientId,
                    sessions.size());
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

        Logger.info("Session: %s closed. zuid:%s, client: %s, %s", session.getId(), userId, client, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        Object client = session.getUserProperties().get("client");
        Object userId = session.getUserProperties().get("zuid");

        Logger.error("Session: %s failed. zuid:%s, client: %s, %s", session.getId(), userId, client, thr.getMessage());
    }
}