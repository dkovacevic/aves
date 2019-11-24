package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    @JsonProperty
    public UUID id;
    @JsonProperty
    public Payload[] payload;
    @JsonProperty("transient")
    public boolean trans;
}
