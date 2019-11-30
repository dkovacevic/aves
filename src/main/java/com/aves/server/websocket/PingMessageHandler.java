package com.aves.server.websocket;

import com.aves.server.Logger;

import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PingMessageHandler implements MessageHandler.Whole<InputStream> {
    private final Session session;

    PingMessageHandler(Session session) {
        this.session = session;
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[1024 * 4];
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }

    @Override
    public void onMessage(InputStream is) {
        try {
            byte[] bytes = toByteArray(is);
            String text = new String(bytes, "UTF-8");
            if (text.equalsIgnoreCase("ping")) {
                Logger.info("Received String: session: %s, text: %s", session.getId(), text);
                session.getBasicRemote().sendBinary(ByteBuffer.wrap("pong".getBytes("UTF-8")));
            }
        } catch (Exception e) {
            Logger.error("PingMessageHandler session: %s, %s", session.getId(), e);
        }
    }
}
