package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class UserIds {
    @JsonProperty("user_ids")
    public List<UUID> usersIds;
}
