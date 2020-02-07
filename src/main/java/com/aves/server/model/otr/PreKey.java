package com.aves.server.model.otr;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class PreKey {
    @NotNull
    public Integer id;

    @NotNull
    @NotEmpty
    public String key;
}
