package com.aves.server.notifications;

import com.aves.server.Aves;
import com.aves.server.model.Configuration;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

@Ignore
public class NotificationPlayground {

    @Test
    public void rawSend() throws IOException, FirebaseMessagingException {

        FileInputStream serviceAccount =
                new FileInputStream("/Users/lukas/work/wire/aves/firebase-sdk.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://wire-bot.firebaseio.com")
                .build();

        FirebaseApp.initializeApp(options);
        String registrationToken = "<SERVICE TOKEN HERE>";

        Message message = Message.builder()
                .putData("score", "850")
                .putData("time", "2:45")
                .setToken(registrationToken)
                .build();

        String response = FirebaseMessaging.getInstance().send(message);

        System.out.println("Successfully sent message: " + response);
    }

    @Test
    public void sendUsingService() {
        Configuration c = new Configuration();
        c.firebaseCredentialsFilePath = "/Users/lukas/work/wire/aves/firebase-sdk.json";
        c.firebaseDatabaseUrl = "https://wire-bot.firebaseio.com";
        Aves.config = c;

        FBNotificationService.getInstance()
                .send(
                        "someUserId",
                        "someData",
                        "<SERVICE TOKEN HERE>");
    }

}
