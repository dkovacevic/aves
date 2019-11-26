package com.aves.server.model;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class Invite {
    @NotNull
    @NotEmpty
    public String email;

    @NotNull
    @NotEmpty
    public String name;

    @NotNull
    @NotEmpty
    public String phone;

    public String firstname;

    public String lastname;

    public String country;

}
