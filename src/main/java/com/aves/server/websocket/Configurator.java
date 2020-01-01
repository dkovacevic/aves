package com.aves.server.websocket;

import com.aves.server.Aves;
import com.aves.server.tools.Util;
import io.jsonwebtoken.Jwts;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.UUID;

public class Configurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        try {
            String token = Util.getQueryParam(request.getQueryString(), "access_token");

            String subject = Jwts.parser()
                    .setSigningKey(Aves.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);
            sec.getUserProperties().put("zuid", userId);
        } catch (Exception e) {
            throw new RuntimeException("WebSocket Configurator", e);
        }
    }
}