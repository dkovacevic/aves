package com.aves.server.websocket;

import com.aves.server.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<Event> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String encode(Event event) throws EncodeException {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EncodeException(event, e.getMessage(), e);
        }
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
