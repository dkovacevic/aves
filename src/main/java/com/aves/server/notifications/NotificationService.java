package com.aves.server.notifications;

import com.aves.server.tools.Logger;

import java.util.UUID;

public abstract class NotificationService {

    private final NotificationProvider provider;

    public NotificationService(NotificationProvider provider) {
        this.provider = provider;
    }

    public void send(UUID userId, String id, String token) throws NotificationException {
        try {
            sendUnsafe(userId, id, token);
        } catch (Exception ex) {
            Logger.debug("Error during sending notification. %s", ex.toString());
            throw new NotificationException(ex.getMessage(), provider, ex);
        }
    }

    protected abstract void sendUnsafe(UUID userId, String id, String token) throws Exception;
}
