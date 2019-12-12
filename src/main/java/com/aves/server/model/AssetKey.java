package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetKey {
    public UUID key;
    public String token;
    public String expires;
}
