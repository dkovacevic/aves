package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.EventSender;
import com.aves.server.model.Device;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.NewClient;
import com.aves.server.model.otr.PreKey;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static com.aves.server.EventSender.sendEvent;

@Api
@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
public class ClientsResource {
    private final ClientsDAO clientsDAO;
    private final PrekeysDAO prekeysDAO;

    public ClientsResource(ClientsDAO clientsDAO, PrekeysDAO prekeysDAO) {
        this.clientsDAO = clientsDAO;
        this.prekeysDAO = prekeysDAO;
    }

    @POST
    @ApiOperation(value = "Register new device")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @ApiParam @Valid NewClient newClient) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            if (clientsDAO.getClients(userId).size() > 7) {
                return Response.
                        ok(new ErrorMessage("Too many devices already")).
                        status(403).
                        build();
            }

            String clientId = Util.nextHex();

            PreKey lastkey = newClient.lastkey;
            clientsDAO.insert(clientId, userId, lastkey.id, newClient);

            for (PreKey preKey : newClient.prekeys) {
                prekeysDAO.insert(clientId, preKey);
            }
            prekeysDAO.insert(clientId, lastkey);

            Logger.info("New Device: %s, Last key: %d", clientId, lastkey.id);

            Device device = clientsDAO.getDevice(userId, clientId);

            Event event = EventSender.userClientAddEvent(device);
            sendEvent(event, userId);

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

    @PUT
    @Path("{client}")
    @ApiOperation(value = "Update a registered client")
    @Authorization("Bearer")
    public Response put(@Context ContainerRequestContext context,
                        @PathParam("client") String clientId,
                        @ApiParam NewClient newClient) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            if (newClient.lastkey != null) {
                PreKey lastkey = newClient.lastkey;
                clientsDAO.insert(clientId, userId, lastkey.id, newClient);
                prekeysDAO.insert(clientId, lastkey);
            }

            if (newClient.prekeys != null) {
                for (PreKey preKey : newClient.prekeys) {
                    prekeysDAO.insert(clientId, preKey);
                }
            }

            Device device = clientsDAO.getDevice(userId, clientId);

            return Response.
                    ok(device).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ClientResource.put : %s", e);
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

    @DELETE
    @Path("{clientId}")
    @ApiOperation(value = "Delete device")
    @Authorization("Bearer")
    public Response delete(@Context ContainerRequestContext context,
                           @PathParam("clientId") String clientId) {
        try {
            clientsDAO.delete(clientId);

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ClientResource.delete : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
