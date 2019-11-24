package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessToken {
    @NotNull
    @JsonProperty("expires_in")
    public long expiresIn;

    @NotNull
    @JsonProperty("access_token")
    public String accessToken;

    @NotNull
    @JsonProperty("token_type")
    public String tokenType;

    @JsonProperty
    public UUID user;
}
