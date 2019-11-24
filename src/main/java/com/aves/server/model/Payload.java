package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payload {
    @JsonProperty
    @NotNull
    public String type;
    @JsonProperty("conversation")
    public UUID convId;
    @JsonProperty
    @NotNull
    public UUID from;
    @JsonProperty
    @NotNull
    public String time;
    @JsonProperty
    @NotNull
    public Data data;
    @JsonProperty
    public UUID team;

    // User Mode
    @JsonProperty
    public Connection connection;
    @JsonProperty
    public User user;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty
        @NotNull
        public String sender;
        @JsonProperty
        @NotNull
        public String recipient;
        @JsonProperty
        public String text;
        @JsonProperty("user_ids")
        public List<UUID> userIds;
        @JsonProperty
        public String name;

        // User Mode
        @JsonProperty
        public String id;
        @JsonProperty
        public String key;
        @JsonProperty
        public UUID user;
        @JsonProperty
        public UUID creator;
        @JsonProperty
        public Members members;
    }

    // User Mode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Connection {
        @JsonProperty
        public String status;

        @JsonProperty
        public UUID from;

        @JsonProperty
        public UUID to;

        @JsonProperty("conversation")
        public UUID convId;
    }

    // User Mode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty
        public UUID id;

        @JsonProperty
        public String name;

        @JsonProperty("accent_id")
        public int accent;

        @JsonProperty
        public String handle;

        @JsonProperty
        public String email;
    }

    // User Mode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Members {
        @JsonProperty
        public List<Member> others;
    }
}
