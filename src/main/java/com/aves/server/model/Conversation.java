package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
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
    public Members members = new Members();

    public static class Members {
        @JsonProperty
        public Self self = new Self();
        @JsonProperty
        public List<Member> others = new ArrayList<>();
    }

    public static class Self {
        public int status;
        public UUID id;
    }
}
