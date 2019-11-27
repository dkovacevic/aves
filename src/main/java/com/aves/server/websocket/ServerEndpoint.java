package com.aves.server.websocket;

import com.aves.server.Aves;
import com.aves.server.DAO.ClientsDAO;
import com.aves.server.Logger;
import com.aves.server.Util;
import com.aves.server.model.Message;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.jsonwebtoken.Jwts;

import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Metered
@Timed
public class ServerEndpoint extends Endpoint {
    private final static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();// ClientID, Session,

    public static boolean send(String clientId, Message message) throws IOException, EncodeException {
        Session session = sessions.get(clientId);
        if (session != null && session.isOpen()) {
            Logger.debug("Sending message (%s) over wss to client: %s",
                    message.id,
                    clientId);

            session.getBasicRemote().sendObject(message);
            return true;
        }
        return false;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
            String clientId = Util.getQueryParam(session.getQueryString(), "client");
            String token = Util.getQueryParam(session.getQueryString(), "access_token");

            if (token == null || clientId == null) {
                Logger.warning("Session %s missing token or clientId", session.getId());
                session.close();
                return;
            }

            String subject = Jwts.parser()
                    .setSigningKey(Aves.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);
            ClientsDAO clientsDAO = Aves.jdbi.onDemand(ClientsDAO.class);
            UUID challenge = clientsDAO.getUserId(clientId);
            if (!userId.equals(challenge)) {
                Logger.warning("Session %s client: %s. Unknown clientId", session.getId(), clientId);
                session.close();
                return;
            }

            Logger.info("Session %s connected. client: %s", session.getId(), clientId);
            sessions.put(clientId, session);
        } catch (Exception e) {

        }
    }
}