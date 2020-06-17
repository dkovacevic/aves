package com.aves.server.notifications;

import com.aves.server.tools.Logger;

public abstract class NotificationService {

    private final NotificationProvider provider;

    public NotificationService(NotificationProvider provider) {
        this.provider = provider;
    }

    public void send(String userId, String data, String token) throws NotificationException {
        try {
            sendUnsafe(userId, data, token);
        } catch (Exception ex) {
            Logger.error("Error during sending notification. %s", ex.toString());
            throw new NotificationException(ex.getMessage(), provider, ex);
        }
    }

    protected abstract void sendUnsafe(String userId, String data, String token) throws Exception;
}
