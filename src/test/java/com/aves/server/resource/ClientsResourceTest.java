package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.model.Device;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static com.aves.server.resource.Const.CLIENT_ID;
import static com.aves.server.resource.Const.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClientsResourceTest {
    private static final ClientsDAO clientsDAO = mock(ClientsDAO.class);
    private static final PrekeysDAO prekeysDAO = mock(PrekeysDAO.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addProvider(AuthenticationFeature.class)
            .addResource(new ClientsResource(clientsDAO, prekeysDAO))
            .build();

    private final Device device = getDevice();

    private Device getDevice() {
        final Device device = new Device();
        device.id = CLIENT_ID;
        device.type = "permanent";
        device.model = "Chrome";
        device.clazz = "Desktop";
        device.lastKey = 0xffff;
        return device;
    }

    @Before
    public void setup() {
        when(clientsDAO.getDevice(USER_ID, CLIENT_ID)).thenReturn(device);
    }

    @After
    public void tearDown() {
        reset(clientsDAO);
    }

    @Test
    public void testGetDevice() {
        final Device actual = resources
                .target("clients")
                .path(CLIENT_ID)
                .request()
                .get(Device.class);

        assertThat(actual.id).isEqualTo(device.id);
        assertThat(actual.type).isEqualTo(device.type);
        assertThat(actual.clazz).isEqualTo(device.clazz);
        assertThat(actual.lastKey).isEqualTo(device.lastKey);
        assertThat(actual.model).isEqualTo(device.model);

        verify(clientsDAO).getDevice(USER_ID, CLIENT_ID);
    }
}
