package com.aves.server.model;

import com.aves.server.model.otr.PreKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewClient {
    @JsonProperty
    public String id;

    @NotNull
    public PreKey lastkey;

    @NotNull
    @NotEmpty
    public ArrayList<PreKey> prekeys;

    public String type;

    @JsonProperty("class")
    public String clazz;

    public String label;

}
