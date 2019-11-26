package com.aves.server.model;

import com.aves.server.model.otr.PreKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewClient {
    @JsonProperty
    public String id;

    @NotNull
    public PreKey lastkey;

    @NotNull
    @NotEmpty
    public ArrayList<PreKey> prekeys;
}
