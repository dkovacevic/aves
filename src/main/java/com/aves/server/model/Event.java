package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event {
    @JsonProperty
    public UUID id;
    @JsonProperty
    public ArrayList<Payload> payload = new ArrayList<>();
    @JsonProperty("transient")
    public Boolean trans;
}
