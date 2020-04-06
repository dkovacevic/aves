package com.aves.server;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.NotificationsDAO;
import com.aves.server.DAO.PendingsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.clients.SwisscomClient;
import com.aves.server.filters.AuthenticationFeature;
import com.aves.server.healthchecks.StatusHealthcheck;
import com.aves.server.model.Configuration;
import com.aves.server.model.Event;
import com.aves.server.model.Payload;
import com.aves.server.model.SignatureEvent;
import com.aves.server.resource.*;
import com.aves.server.resource.dummy.CallsResource;
import com.aves.server.resource.dummy.OnboardingResource;
import com.aves.server.resource.dummy.TeamsResource;
import com.aves.server.tools.Logger;
import com.aves.server.websocket.Configurator;
import com.aves.server.websocket.EventEncoder;
import com.aves.server.websocket.ServerEndpoint;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.mtakaki.dropwizard.admin.AdminResourceBundle;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.jersey.protobuf.ProtobufBundle;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.jsonwebtoken.security.Keys;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

import javax.crypto.SecretKey;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import static com.aves.server.tools.Util.time;

public class Aves extends Application<Configuration> {
    private final AdminResourceBundle admin = new AdminResourceBundle();

    private static SecretKey key;
    public static Jdbi jdbi;
    public Client jerseyClient;
    public static Configuration config;

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
        bootstrap.addBundle(new JdbiExceptionsBundle());

        ServerEndpointConfig config = ServerEndpointConfig
                .Builder
                .create(ServerEndpoint.class, "/await")
                .configurator(new Configurator())
                .encoders(Collections.singletonList(EventEncoder.class))
                .build();

        bootstrap.addBundle(new WebsocketBundle(config));
        bootstrap.addBundle(new ProtobufBundle());
    }

    public void run(Configuration config, Environment environment) {
        Aves.config = config;

        DataSourceFactory database = config.database;

        // Migrate DB if needed
        Flyway flyway = Flyway
                .configure()
                .dataSource(database.getUrl(), database.getUser(), database.getPassword())
                .load();
        flyway.migrate();

        Aves.key = Keys.hmacShaKeyFor(config.key.getBytes());

        jdbi = new JdbiFactory().build(environment, database, "aves");

        ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);
        PrekeysDAO prekeysDAO = jdbi.onDemand(PrekeysDAO.class);
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);

        EventSender.clientsDAO = clientsDAO;
        EventSender.notificationsDAO = notificationsDAO;

        // Enable CORS headers
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "*");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        jerseyClient = new JerseyClientBuilder(environment)
                .using(config.jerseyConfig)
                .withProvider(JacksonJsonProvider.class)
                .build(getName());

        SwisscomClient swisscomClient = new SwisscomClient(jerseyClient);

        environment.jersey().register(AuthenticationFeature.class);

        //admin.getJerseyEnvironment().register(new StatusHealthcheck());
        environment.jersey().register(new StatusHealthcheck());

        environment.jersey().register(new ProtocolBufferMessageBodyProvider());

        environment.jersey().register(new ConfigResource());
        environment.jersey().register(new LoginResource(jdbi, config));
        environment.jersey().register(new RegisterResource(jdbi));
        environment.jersey().register(new ClientsResource(clientsDAO, prekeysDAO));
        environment.jersey().register(new ConversationsResource(jdbi));
        environment.jersey().register(new MessagesResource(jdbi));
        environment.jersey().register(new PrekeysResource(jdbi));
        environment.jersey().register(new AccessResource(config));
        environment.jersey().register(new AssetsResource());
        environment.jersey().register(new UsersResource(jdbi));
        environment.jersey().register(new SelfResource(jdbi));
        environment.jersey().register(new InviteResource(jdbi));
        environment.jersey().register(new NotificationsResource(jdbi));
        environment.jersey().register(new SignatureResource(jdbi, swisscomClient));
        environment.jersey().register(new SearchResource(jdbi));
        environment.jersey().register(new ConnectionsResource(jdbi));

        // Dummies
        environment.jersey().register(new TeamsResource());
        environment.jersey().register(new PropertiesResource(jdbi));
        environment.jersey().register(new CallsResource());
        environment.jersey().register(new OnboardingResource());

//        environment.lifecycle()
//                .scheduledExecutorService("pullingManager")
//                .build()
//                .scheduleWithFixedDelay(() -> pull(swisscomClient, jdbi.onDemand(PendingsDAO.class)), 10, 5, TimeUnit.SECONDS);
    }

    private void pull(SwisscomClient swisscomClient, PendingsDAO pendingsDAO) {
        for (UUID responseId : pendingsDAO.getRequests()) {
            try {
                SwisscomClient.SignResponse res = swisscomClient.pending(responseId);
                if (res != null && res.signature != null) {
                    Payload<SignatureEvent> payload = new Payload<>();
                    payload.type = "signature.available";
                    payload.time = time();
                    payload.data = new SignatureEvent();
                    payload.data.responseId = responseId;

                    Event event = new Event();
                    event.id = UUID.randomUUID();
                    event.payload.add(payload);

                    UUID userId = pendingsDAO.getUserId(responseId);

                    EventSender.sendEvent(event, userId);

                    pendingsDAO.delete(responseId);

                    Logger.info("SignaturePull.sendEvent: user: %s, respId: %s", userId, responseId);
                }
            } catch (IOException e) {
                Logger.error("SignaturePull: ", e);
            }
        }
    }

    public Client getClient() {
        return jerseyClient;
    }

}
