package com.aves.server.notifications;

import com.aves.server.Aves;
import com.aves.server.model.Configuration;
import com.aves.server.tools.Logger;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessagingException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class NotificationService {

    private static NotificationService INSTANCE = null;

    public static NotificationService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = buildInstance(Aves.config);
        }
        return INSTANCE;
    }

    private static NotificationService buildInstance(Configuration config) {
        try {
            InputStream inputStream = new FileInputStream(config.firebaseCredentialsFilePath);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .setDatabaseUrl(config.firebaseDatabaseUrl)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            return new NotificationServiceImpl(app);
        } catch (IOException ex) {
            Logger.error("It was not possible to create instance of Notification Service: %s\n%s", ex.getMessage(), ex.toString());
            return null;
        }
    }

    public abstract void send(String userId, String data, String token) throws FirebaseMessagingException;
}
