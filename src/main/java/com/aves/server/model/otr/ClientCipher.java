package com.aves.server.model.otr;

import java.util.HashMap;

// <ClientId, Cipher> // cipher is base64 encoded
public class ClientCipher extends HashMap<String, String> {

    public String get(String clientId) {
        return super.get(clientId);
    }
}
