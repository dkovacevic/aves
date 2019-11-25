package com.aves.server.websocket;


import com.aves.server.DAO.ClientsDAO;
import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.Message;
import io.jsonwebtoken.Jwts;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(
        value = "/aves/await/{token}/{clientId}",
        encoders = MessageEncoder.class
)
public class WebSocket {
    private final static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();// ClientID, Session,

    public static boolean send(String clientId, Message message) throws IOException, EncodeException {
        Session session = sessions.get(clientId);
        if (session != null && session.isOpen()) {
            Logger.info("Sending message (%s) over wss to client: %s",
                    message.id,
                    clientId);

            session.getBasicRemote().sendObject(message);
            return true;
        }
        return false;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token, @PathParam("clientId") String clientId) throws IOException {
        try {
            String subject = Jwts.parser()
                    .setSigningKey(Server.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);
            ClientsDAO clientsDAO = Server.jdbi.onDemand(ClientsDAO.class);
            UUID challenge = clientsDAO.getUserId(clientId);
            if (!userId.equals(challenge)) {
                Logger.warning("Session %s client: %s. Unknown clientId", session.getId(), clientId);
                session.close();
                return;
            }

            sessions.put(clientId, session);
            Logger.debug("Session %s connected. client: %s", session.getId(), clientId);
        } catch (Exception e) {
            Logger.warning("onOpen: client: %s err: %s", clientId, e);
            session.close();
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        Logger.debug("%s disconnected", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        Logger.warning("%s error: %s", session.getId(), throwable);
    }
}