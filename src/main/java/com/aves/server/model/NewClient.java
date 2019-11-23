package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

public class NewClient {
    @NotNull
    @NotEmpty
    public String cookie;

    @NotNull
    @NotEmpty
    public String type;

    @NotNull
    @JsonProperty("class")
    @NotEmpty
    public String clazz;

    @NotNull
    public PreKey lastkey;

    @NotNull
    @NotEmpty
    public ArrayList<PreKey> prekeys;
}
