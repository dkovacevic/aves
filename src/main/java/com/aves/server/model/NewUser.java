package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class NewUser {
    @NotNull
    @Length(min = 6, max = 1024)
    public String email;

    @NotNull
    @NotEmpty
    public String name;

    @NotNull
    @Length(min = 6, max = 1024)
    public String password;

    @NotNull
    @NotEmpty
    public String phone;

    public String firstname;

    public String lastname;

    public String country;

    @JsonProperty("accent_id")
    public int accent;
}
