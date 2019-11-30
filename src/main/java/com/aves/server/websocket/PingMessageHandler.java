package com.aves.server.websocket;

import com.aves.server.tools.Logger;

import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.aves.server.tools.Util.toByteArray;

public class PingMessageHandler implements MessageHandler.Whole<InputStream> {
    private final Session session;

    PingMessageHandler(Session session) {
        this.session = session;
    }

    @Override
    public void onMessage(InputStream is) {
        try {
            byte[] bytes = toByteArray(is);
            String text = new String(bytes, "UTF-8");
            if (text.equalsIgnoreCase("ping")) {
                Logger.debug("Received String: session: %s, text: %s", session.getId(), text);
                session.getBasicRemote().sendBinary(ByteBuffer.wrap("pong".getBytes("UTF-8")));
            }
        } catch (Exception e) {
            Logger.error("PingMessageHandler session: %s, %s", session.getId(), e);
        }
    }
}
