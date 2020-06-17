package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.OneOf;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushToken {
    @JsonProperty
    @NotNull
    public String token;
    @JsonProperty
    @NotNull
    public String app;
    @JsonProperty
    public String client;
    @JsonProperty
    @OneOf(value = {"GCM", "APNS", "APNS_SANDBOX", "APNS_VOIP", "APNS_VOIP_SANDBOX"})
    public String transport;
}
