package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class Configuration extends io.dropwizard.Configuration {
    @JsonProperty("swagger")
    public SwaggerBundleConfiguration swagger;

    @Valid
    @NotNull
    @JsonProperty("jerseyClient")
    public JerseyClientConfiguration jerseyConfig = new JerseyClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    public DataSourceFactory database;

    @Valid
    @NotNull
    @JsonProperty
    public String key;

    @JsonProperty
    public long tokenExpiration = 30;
}
