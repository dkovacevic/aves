package com.aves.server.model;

import javax.validation.constraints.NotNull;

public class NewUser {
    @NotNull
    public String email;

    @NotNull
    public String name;

    @NotNull
    public String password;
}
