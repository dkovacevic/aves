package com.aves.server;

import com.aves.server.model.Configuration;
import com.aves.server.resource.*;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.jsonwebtoken.security.Keys;
import org.skife.jdbi.v2.DBI;

import javax.crypto.SecretKey;

public class Server extends Application<Configuration> {
    private static SecretKey key;

    public static SecretKey getKey() {
        return key;
    }

    public static void main(String[] args) throws Exception {
        new Server().run(args);
    }
    
    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addBundle(new SwaggerBundle<Configuration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(Configuration configuration) {
                return configuration.swagger;
            }
        });
    }

    public void run(Configuration config, Environment environment) throws Exception {
        Server.key = Keys.hmacShaKeyFor(config.key.getBytes());
        DBI jdbi = new DBIFactory().build(environment, config.database, "postgresql");

        environment.jersey().register(new LoginResource(jdbi));
        environment.jersey().register(new RegisterResource(jdbi));
        environment.jersey().register(new ClientResource(jdbi));
        environment.jersey().register(new ConversationsResource(jdbi));
        environment.jersey().register(new MessagesResource(jdbi));
    }
}
