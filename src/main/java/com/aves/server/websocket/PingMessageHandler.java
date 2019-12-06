package com.aves.server.websocket;

import com.aves.server.tools.Logger;

import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.nio.ByteBuffer;

public class PingMessageHandler implements MessageHandler.Whole<String> {
    private final Session session;

    PingMessageHandler(Session session) {
        this.session = session;
    }

    @Override
    public void onMessage(String ping) {
        try {
            if (ping.equalsIgnoreCase("ping")) {
                Object client = session.getUserProperties().get("client");
                Object userId = session.getUserProperties().get("zuid");

                Logger.debug("Received ping: session: %s, zuid: %s, client: %s", session.getId(), userId, client);
                session.getBasicRemote().sendBinary(ByteBuffer.wrap("pong".getBytes()));
            }
        } catch (Exception e) {
            Logger.error("PingMessageHandler session: %s, %s", session.getId(), e);
        }
    }
}
