package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class Configuration extends io.dropwizard.Configuration {
    @JsonProperty("swagger")
    @NotNull
    public SwaggerBundleConfiguration swagger;

    @Valid
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
    public long tokenExpiration = 900;

    @JsonProperty
    @NotNull
    public String sendgridApiKey;
    @JsonProperty
    @NotNull
    public String minioURL;
    @JsonProperty
    @NotNull
    public String minioAccessKey;
    @JsonProperty
    @NotNull
    public String minioSecretKey;


    @JsonProperty
    @NotNull
    public String firebaseCredentialsFilePath;

    @JsonProperty
    @NotNull
    public String firebaseDatabaseUrl;

    public String baseURL;
}
