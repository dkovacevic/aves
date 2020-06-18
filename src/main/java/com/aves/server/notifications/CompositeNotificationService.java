package com.aves.server.notifications;

import com.aves.server.model.PushToken;

public class CompositeNotificationService {

    private static CompositeNotificationService INSTANCE;

    private CompositeNotificationService() {
    }

    public static CompositeNotificationService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CompositeNotificationService();
        }
        return INSTANCE;
    }

    public void send(String userId, String id, PushToken token) {
        if (isAndroid(token)) {
            FBNotificationService.getInstance().send(userId, id, token.token);
        }
    }

    private Boolean isAndroid(PushToken token) {
        return token.transport.equalsIgnoreCase("GCM");
    }
}
