package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewUser {
    @NotNull
    @Length(min = 6)
    public String email;

    @NotNull
    @Length(min = 3, max = 125)
    public String name;

    @NotNull
    @Length(min = 10, max = 16)
    public String phone;

    @NotNull
    @Length(max = 124)
    public String firstname;

    @NotNull
    @Length(max = 124)
    public String lastname;

    @NotNull
    @Length(min = 2, max = 2)
    public String country;

    public String password;
}
