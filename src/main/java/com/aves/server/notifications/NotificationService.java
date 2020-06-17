package com.aves.server.notifications;

import com.aves.server.tools.Logger;

public abstract class NotificationService {

    private final NotificationProvider provider;

    public NotificationService(NotificationProvider provider) {
        this.provider = provider;
    }

    public void send(String userId, String id, String token) throws NotificationException {
        try {
            sendUnsafe(userId, id, token);
        } catch (Exception ex) {
            Logger.error("Error during sending notification. %s", ex.toString());
            throw new NotificationException(ex.getMessage(), provider, ex);
        }
    }

    protected abstract void sendUnsafe(String userId, String id, String token) throws Exception;
}
