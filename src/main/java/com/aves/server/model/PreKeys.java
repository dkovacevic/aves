package com.aves.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PreKeys extends HashMap<UUID, HashMap<String, PreKey>> {
    public PreKeys() {
    }

    public PreKeys(ArrayList<PreKey> array, String clientId, UUID userId) {
        super();

        HashMap<String, PreKey> devs = new HashMap<>();
        for (PreKey key : array) {
            devs.put(clientId, key);
        }
        put(userId, devs);
    }

}