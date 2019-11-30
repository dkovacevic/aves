package com.aves.server;

import com.aves.server.clients.SwisscomClient;
import com.aves.server.filters.AuthenticationFeature;
import com.aves.server.healthchecks.StatusHealthcheck;
import com.aves.server.model.Configuration;
import com.aves.server.resource.*;
import com.aves.server.resource.dummy.CallsResource;
import com.aves.server.resource.dummy.PropertiesResource;
import com.aves.server.resource.dummy.TeamsResource;
import com.aves.server.websocket.EventEncoder;
import com.aves.server.websocket.ServerEndpoint;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.mtakaki.dropwizard.admin.AdminResourceBundle;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.jsonwebtoken.security.Keys;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;

import javax.crypto.SecretKey;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.websocket.Encoder;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.EnumSet;

public class Aves extends Application<Configuration> {
    private final AdminResourceBundle admin = new AdminResourceBundle();

    private static SecretKey key;
    public static DBI jdbi;

    public static SecretKey getKey() {
        return key;
    }

    public static void main(String[] args) throws Exception {
        new Aves().run(args);
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
        bootstrap.addBundle(admin);

        ArrayList<Class<? extends Encoder>> encoders = new ArrayList<>();
        encoders.add(EventEncoder.class);

        final ServerEndpointConfig config = ServerEndpointConfig
                .Builder
                .create(ServerEndpoint.class, "/aves/await")
                .encoders(encoders)
                .build();

        WebsocketBundle bundle = new WebsocketBundle(config);
        bootstrap.addBundle(bundle);
    }

    public void run(Configuration config, Environment environment) throws InvalidPortException, InvalidEndpointException {
        Aves.key = Keys.hmacShaKeyFor(config.key.getBytes());
        jdbi = new DBIFactory().build(environment, config.database, "postgresql");

        // Enable CORS headers
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "*");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        Client jerseyClient = new JerseyClientBuilder(environment)
                .using(config.jerseyConfig)
                .withProvider(JacksonJsonProvider.class)
                .build(getName());

        SwisscomClient swisscomClient = new SwisscomClient(jerseyClient);

        environment.jersey().register(AuthenticationFeature.class);

        //admin.getJerseyEnvironment().register(new StatusHealthcheck());
        environment.jersey().register(new StatusHealthcheck());

        environment.jersey().register(new LoginResource(jdbi, config));
        environment.jersey().register(new RegisterResource(jdbi));
        environment.jersey().register(new ClientsResource(jdbi));
        environment.jersey().register(new ConversationsResource(jdbi));
        environment.jersey().register(new MessagesResource(jdbi));
        environment.jersey().register(new PrekeysResource(jdbi));
        environment.jersey().register(new AccessResource(jdbi, config));
        environment.jersey().register(new AssetsResource());
        environment.jersey().register(new UsersResource(jdbi));
        environment.jersey().register(new SelfResource(jdbi));
        environment.jersey().register(new InviteResource(jdbi));
        environment.jersey().register(new NotificationsResource(jdbi));
        environment.jersey().register(new SignatureResource(jdbi, swisscomClient));

        // Dummies
        environment.jersey().register(new TeamsResource());
        environment.jersey().register(new PropertiesResource());
        environment.jersey().register(new CallsResource());

    }
}
