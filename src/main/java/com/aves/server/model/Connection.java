package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    public String status = "accepted";
    @JsonProperty("last_update")
    public String time;
    public UUID from;
    public UUID to;
    public UUID conversation;
    public String message = " ";
}
