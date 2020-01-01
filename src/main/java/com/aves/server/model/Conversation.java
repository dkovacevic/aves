package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Conversation {
    @JsonProperty
    public UUID id;

    @JsonProperty
    public String name;

    @JsonProperty
    public UUID creator;

    @JsonProperty
    @NotNull
    public Members members = new Members();

    @JsonProperty
    public int type;

    @JsonProperty("last_event_time")
    public String lastEventTime;
    @JsonProperty("last_event")
    public String lastEvent;
}
