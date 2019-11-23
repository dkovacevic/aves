package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorMessage {
    @JsonProperty
    public String message;

    public ErrorMessage(String message) {
        this.message = message;
    }
}