package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationList {
    @JsonProperty("has_more")
    public boolean hasMore;

    @JsonProperty
    public ArrayList<Event> notifications = new ArrayList<>();

    @JsonProperty
    public String time;
}
