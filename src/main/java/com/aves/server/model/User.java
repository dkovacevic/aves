package com.aves.server.model;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class User {
    @NotNull
    public UUID id;

    @NotNull
    @NotEmpty
    public String email;

    @NotNull
    @NotEmpty
    public String name;

    @NotNull
    @NotEmpty
    public String phone;
}
