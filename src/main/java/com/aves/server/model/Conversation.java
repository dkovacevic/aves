package com.aves.server.model;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.UUID;

public class Conversation {
    public String name;

    @NotNull
    public ArrayList<UUID> users;
}
