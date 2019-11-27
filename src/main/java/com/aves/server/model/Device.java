package com.aves.server.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Device {
    public String id;
    public String time;
    public String type;
    @JsonProperty("class")
    public String clazz;
    public String label;
}
