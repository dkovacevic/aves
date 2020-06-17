package com.aves.server.notifications;

import com.aves.server.Aves;
import com.aves.server.model.Configuration;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class FBNotificationService extends NotificationService {

    private static FBNotificationService INSTANCE = null;

    @SuppressWarnings("unused") // not true, we need to guarantee that we already connected to the firebase
    private FBNotificationService(FirebaseApp app) {
        super(NotificationProvider.FIREBASE);
    }

    public static FBNotificationService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = buildInstance(Aves.config);
        }
        return INSTANCE;
    }

    private static FBNotificationService buildInstance(Configuration config) {
        try {
            InputStream inputStream = new FileInputStream(config.firebaseCredentialsFilePath);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .setDatabaseUrl(config.firebaseDatabaseUrl)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            return new FBNotificationService(app);
        } catch (IOException ex) {
            Logger.error("It was not possible to create instance of Notification Service: %s\n%s", ex.getMessage(), ex.toString());
            throw new NotificationException("Could not create FBNotificationService", NotificationProvider.FIREBASE, ex);
        }
    }


    public void sendUnsafe(String userId, String id, String token) throws FirebaseMessagingException, JsonProcessingException {
        Logger.debug("Sending notification fro user %s", userId);

        Map<String, String> dataMap = new LinkedHashMap<>();
        dataMap.put("id", id);
        String data = new ObjectMapper().writeValueAsString(dataMap);

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
