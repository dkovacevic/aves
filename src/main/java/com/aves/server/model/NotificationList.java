package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationList {
    @JsonProperty("has_more")
    public boolean hasMore;

    @JsonProperty
    public List<Event> notifications = new ArrayList<>();

    @JsonProperty
    public String time;
}
