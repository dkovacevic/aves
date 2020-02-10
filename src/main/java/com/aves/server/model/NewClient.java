package com.aves.server.model;

import com.aves.server.model.otr.PreKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewClient {
    @JsonProperty
    @NotNull
    public PreKey lastkey;

    @NotNull
    public List<PreKey> prekeys;

    @JsonProperty
    public String type;

    @JsonProperty("class")
    public String clazz;

    @JsonProperty
    public String label;

    @JsonProperty
    public String model;

    @JsonProperty
    public String cookie;
}
