package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payload {
    @JsonProperty
    public String type;
    @JsonProperty("conversation")
    public UUID convId;
    @JsonProperty
    public UUID from;
    @JsonProperty
    public String time;
    @JsonProperty
    public Data data;

    @JsonProperty("client")
    public Device device;

    public User user;
    public Connection connection;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Data {
        @JsonProperty
        public String sender;
        @JsonProperty
        public String recipient;
        @JsonProperty
        public String text;
        @JsonProperty("user_ids")
        public List<UUID> userIds;
        @JsonProperty
        public String name;

        // User Mode
        @JsonProperty
        public UUID id;
        @JsonProperty
        public UUID creator;
        @JsonProperty
        public Members members;
        @JsonProperty
        public Integer type;
    }
}
