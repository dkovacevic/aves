package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payload<T> {
    @JsonProperty
    public String type;
    @JsonProperty("conversation")
    public UUID convId;
    @JsonProperty
    public UUID from;
    @JsonProperty
    public String time;

    @JsonProperty
    public T data;

    @JsonProperty("client")
    public Device device;

    @JsonProperty
    public User user;

    @JsonProperty
    public Connection connection;

}
