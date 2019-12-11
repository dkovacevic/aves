package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationList {
    @JsonProperty("has_more")
    @NotNull
    public boolean hasMore;

    @JsonProperty
    @NotNull
    public ArrayList<Event> notifications = new ArrayList<>();
}
