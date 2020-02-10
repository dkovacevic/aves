package com.aves.server.integrations;

import com.aves.server.Aves;
import com.aves.server.model.Configuration;
import com.aves.server.model.Device;
import com.aves.server.model.NewClient;
import com.aves.server.model.otr.PreKey;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.jsonwebtoken.Jwts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTests {
    private static final DropwizardTestSupport<Configuration> SUPPORT = new DropwizardTestSupport<>(
            Aves.class, "aves.yaml",
            ConfigOverride.config("key", "TcZA2Kq4GaOcIbQuOvasrw34321cZAfLW4Ga54fsds43hUuOdcdm42"),
            ConfigOverride.config("jerseyClient.tls.keyStorePath", ""));

    @Before
    public void beforeClass() {
        SUPPORT.before();
    }

    @After
    public void afterClass() {
        SUPPORT.after();
    }

    @Test
    public void testCreateDevice() throws Exception {
        NewClient newClient = new NewClient();
        newClient.type = "permanent";
        newClient.lastkey = new PreKey();
        newClient.lastkey.id = 0xffff;
        newClient.lastkey.key = "dadfdssdfs233redscwe";
        PreKey key = new PreKey();
        key.id = 1;
        key.key = "sdca234mcdskcas34234mxcxk3424";
        newClient.prekeys = Collections.singletonList(key);

        String token = Jwts.builder()
                .setSubject("" + UUID.randomUUID())
                .signWith(Aves.getKey())
                .compact();

        Aves aves = SUPPORT.getApplication();

        final Response res = aves.jerseyClient
                .target("http://localhost:" + SUPPORT.getLocalPort())
                .path("clients")
                .request()
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(newClient, MediaType.APPLICATION_JSON_TYPE));

        final Device device = res.readEntity(Device.class);

        assertThat(device.id).isNotNull();
    }
}