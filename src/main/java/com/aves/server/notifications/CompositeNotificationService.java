package com.aves.server.notifications;

import com.aves.server.model.PushToken;
import com.aves.server.tools.Logger;

public class CompositeNotificationService {

    private static CompositeNotificationService INSTANCE;
    // TODO change this once we are able to actually send notifications
    private final static boolean SENDING_SUPPORTED = false;

    private CompositeNotificationService() {
    }

    public static CompositeNotificationService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CompositeNotificationService();
        }
        return INSTANCE;
    }

    public void send(String userId, String id, PushToken token) {
        try {
            sendUnchecked(userId, id, token);
        } catch (NotificationException ex) {
            if (SENDING_SUPPORTED) {
                Logger.error("Error during sending notification. %s", ex.getMessage());
                throw ex;
            }
        }
    }

    private void sendUnchecked(String userId, String id, PushToken token) {
        if (isAndroid(token)) {
            FBNotificationService.getInstance().send(userId, id, token.token);
        }
    }

    private Boolean isAndroid(PushToken token) {
        return token.transport.equalsIgnoreCase("GCM");
    }
}
