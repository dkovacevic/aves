package com.aves.server.notifications;

import com.aves.server.tools.Logger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

public class NotificationServiceImpl extends NotificationService {

    @SuppressWarnings("unused") // not true, we need to guarantee that we already connected to the firebase
    public NotificationServiceImpl(FirebaseApp app) {
    }

    public void send(String userId, String data, String token) throws FirebaseMessagingException {
        Logger.debug("Sending notification fro user %s", userId);
        Message message = Message.builder()
                .putData("user", userId)
                .putData("data", data)
                .putData("type", "notice")
                .setToken(token)
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        Logger.debug("Response: %s", response);
    }
}
