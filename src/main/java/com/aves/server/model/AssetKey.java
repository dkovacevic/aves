package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetKey {
    public String key;
    public String token;
    public String expires;
    public String domain = "aves.com";
}
