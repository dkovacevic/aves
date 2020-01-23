package com.aves.server.model.otr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewOtrMessage {
    @JsonProperty
    @NotNull
    public String sender; //clientId of the sender

    @JsonProperty
    @NotNull
    public Recipients recipients;
}
