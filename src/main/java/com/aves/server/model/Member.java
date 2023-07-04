package com.aves.server.model;

import com.aves.server.Aves;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Member {
    @JsonProperty
    public UUID id;

    @JsonProperty
    public int status;

    @JsonProperty("qualified_id")
    public QualifiedId getQualifiedId() {
        return new QualifiedId(id, Aves.config.domain);
    }
}
