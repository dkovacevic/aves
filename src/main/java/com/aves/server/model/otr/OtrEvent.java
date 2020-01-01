package com.aves.server.model.otr;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OtrEvent {
    @JsonProperty
    public String sender;
    @JsonProperty
    public String recipient;
    @JsonProperty
    public String text;
}
