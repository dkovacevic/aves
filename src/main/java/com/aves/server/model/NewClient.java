package com.aves.server.model;

import com.aves.server.model.otr.PreKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewClient {
    @JsonProperty
    public String id;

    @JsonProperty
    @NotNull
    public PreKey lastkey;

    @NotNull
    public ArrayList<PreKey> prekeys;

    @JsonProperty
    public String type;

    @JsonProperty("class")
    public String clazz;

    @JsonProperty
    public String label;

}
