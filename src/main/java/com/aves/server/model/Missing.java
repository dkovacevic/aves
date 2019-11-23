package com.aves.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

//<UserId, [ClientId]>
public class Missing extends HashMap<UUID, Collection<String>> {
    public Collection<String> toClients(UUID userId) {
        return get(userId);
    }

    public Collection<UUID> toUserIds() {
        return keySet();
    }

    public void add(UUID userId, String clientId) {
        Collection<String> clients = computeIfAbsent(userId, k -> new ArrayList<>());
        clients.add(clientId);
    }

    public void add(UUID userId, Collection<String> clients) {
        Collection<String> old = computeIfAbsent(userId, k -> new ArrayList<>());
        old.addAll(clients);
    }
}
