package com.aves.server.resource;

import com.aves.server.Aves;
import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.filters.AuthenticationFeature;
import com.aves.server.model.Configuration;
import com.aves.server.model.Device;
import com.aves.server.tools.Util;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClientsResourceTest {
    private static final ClientsDAO clientsDAO = mock(ClientsDAO.class);
    private static final PrekeysDAO prekeysDAO = mock(PrekeysDAO.class);
    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ClientsResource(clientsDAO, prekeysDAO))
            .addProvider(AuthenticationFeature.class)
            .build();
    @ClassRule
    public static DropwizardAppRule<Configuration> app = new DropwizardAppRule<>(Aves.class,
            "aves.yaml",
            ConfigOverride.config("key", "AcZA23q1GaOcIbQuOCcdm1cZAfLW4GaOd1hUuOdcdM2"),
            ConfigOverride.config("jerseyClient.tls.keyStorePath", ""));
    private final UUID userId = UUID.randomUUID();
    private final String clientId = Util.nextHex();
    private final Device device = getDevice();

    private Device getDevice() {
        final Device device = new Device();
        device.id = clientId;
        device.type = "permanent";
        device.model = "Chrome";
        device.clazz = "Desktop";
        device.lastKey = 0xffff;
        return device;
    }

    @Before
    public void setup() {
        when(clientsDAO.getDevice(userId, clientId)).thenReturn(device);
    }

    @After
    public void tearDown() {
        reset(clientsDAO);
    }

    @Test
    public void testGetPerson() {
        String token = Jwts.builder()
                .setSubject("" + userId)
                .signWith(Aves.getKey())
                .compact();

        final Device actual = resources
                .target("clients")
                .path(clientId)
                .request()
                .header("Authorization", "Bearer " + token)
                .get(Device.class);

        assertThat(actual.id).isEqualTo(device.id);
        assertThat(actual.type).isEqualTo(device.type);
        assertThat(actual.clazz).isEqualTo(device.clazz);
        assertThat(actual.lastKey).isEqualTo(device.lastKey);
        assertThat(actual.model).isEqualTo(device.model);

        verify(clientsDAO).getDevice(userId, clientId);
    }
}
