package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorMessage {
    @JsonProperty
    public String message;

    @JsonProperty
    public Integer code;

    @JsonProperty
    public String label = "some_error";

    public ErrorMessage(String message, int code, String label) {
        this.message = message;
        this.code = code;
        this.label = label;
    }

    public ErrorMessage(String message) {
        this.message = message;
    }
}