package com.aves.server.websocket;

import com.aves.server.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class EventEncoder implements Encoder.Binary<Event> {
    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(EndpointConfig endpointConfig) {
        // Custom initialization logic
    }

    @Override
    public void destroy() {
        // Close resources
    }

    @Override
    public ByteBuffer encode(Event event) throws EncodeException {
        try {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                mapper.writeValue(baos, event);
                return ByteBuffer.wrap(baos.toByteArray());
            }
        } catch (Exception e) {
            throw new EncodeException(event, e.getMessage(), e);
        }
    }
}
