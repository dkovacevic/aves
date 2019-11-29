package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.Logger;
import com.aves.server.model.Device;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.NewClient;
import com.aves.server.model.otr.PreKey;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static com.aves.server.Util.next;

@Api
@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
public class ClientsResource {
    private final DBI jdbi;

    public ClientsResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Register new device")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @ApiParam @Valid NewClient newClient) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);
            PrekeysDAO prekeysDAO = jdbi.onDemand(PrekeysDAO.class);

            if (clientsDAO.getClients(userId).size() > 7) {
                return Response.
                        ok(new ErrorMessage("Too many devices already")).
                        status(403).
                        build();
            }

            String clientId = next(12);

            PreKey lastkey = newClient.lastkey;
            clientsDAO.insert(clientId, userId, lastkey.id);

            for (PreKey preKey : newClient.prekeys) {
                prekeysDAO.insert(clientId, preKey.id, preKey.key);
            }
            prekeysDAO.insert(clientId, lastkey.id, lastkey.key);

            Logger.info("New Device: %s, Last key: %d", clientId, lastkey.id);

            Device device = clientsDAO.getDevice(userId, clientId);
            device.cookie = device.label;
            
            return Response.
                    ok(device).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ClientResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @ApiOperation(value = "Get all devices")
    @Authorization("Bearer")
    public Response getAll(@Context ContainerRequestContext context) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            List<Device> devices = clientsDAO.getDevices(userId);

            return Response.
                    ok(devices).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ClientResource.getAll : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("{clientId}")
    @ApiOperation(value = "Get device")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context,
                        @PathParam("clientId") String clientId) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            Device device = clientsDAO.getDevice(userId, clientId);

            return Response.
                    ok(device).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ClientResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
