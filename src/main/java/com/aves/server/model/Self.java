package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Self {
    @JsonProperty
    public int status;
    @JsonProperty
    public UUID id;
    @JsonProperty("otr_archived")
    public boolean archived;
    @JsonProperty("status_ref")
    public String statusRef = "0.0";
}
