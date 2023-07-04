package com.aves.server.model;

import java.util.UUID;

public class QualifiedId {
    public UUID id;
    public String domain;

    public QualifiedId(UUID id, String domain) {
        this.id = id;
        this.domain = domain;
    }

    public QualifiedId() {
    }
}
