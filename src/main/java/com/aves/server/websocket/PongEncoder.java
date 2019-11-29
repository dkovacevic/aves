package com.aves.server.websocket;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.PongMessage;
import java.nio.ByteBuffer;

public class PongEncoder implements Encoder.Binary<PongMessage> {
    @Override
    public ByteBuffer encode(PongMessage event) throws EncodeException {
        return event.getApplicationData();
    }

    @Override
    public void init(EndpointConfig endpointConfig) {
        // Custom initialization logic
    }

    @Override
    public void destroy() {
        // Close resources
    }
}
