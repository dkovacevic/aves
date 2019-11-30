package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.model.Device;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.otr.Missing;
import com.aves.server.model.otr.PreKey;
import com.aves.server.model.otr.PreKeys;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Api
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class PrekeysResource {
    //private static final int MAX_PREKEY_ID = 0xFFFF;
    private final DBI jdbi;

    public PrekeysResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @Path("prekeys")
    @ApiOperation(value = "Get prekeys for users/clients")
    @Authorization("Bearer")
    public Response post(@ApiParam @Valid Missing missing) {

        try {
            PrekeysDAO prekeysDAO = jdbi.onDemand(PrekeysDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            PreKeys preKeys = new PreKeys();

            for (UUID userId : missing.keySet()) {
                HashMap<String, PreKey> map = new HashMap<>();
                for (String clientId : missing.get(userId)) {
                    PreKey preKey = prekeysDAO.get(clientId);
                    if (preKey == null) {
                        Logger.error("PrekeysResource.post: No prekeys for client: %s", clientId);
                        continue;
                    }

                    Device device = clientsDAO.getDevice(userId, clientId);
                    if (device == null) {
                        Logger.error("PrekeysResource.post: User: %s does not have client: %s", userId, clientId);
                        continue;
                    }

                    if (preKey.id != device.lastKey) {
                        prekeysDAO.mark(clientId, preKey.id);
                    }

                    map.put(clientId, preKey);

                }
                preKeys.put(userId, map);
            }

            return Response.
                    ok(preKeys).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PrekeysResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("{userId}/prekeys/{clientId}")
    @ApiOperation(value = "Get prekey for user and client")
    @Authorization("Bearer")
    public Response getPrekey(@PathParam("userId") UUID userId, @PathParam("clientId") String clientId) {

        try {
            PrekeysDAO prekeysDAO = jdbi.onDemand(PrekeysDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            UUID challenge = clientsDAO.getUserId(clientId);
            if (!Objects.equals(userId, challenge)) {
                Logger.warning("PrekeysResource.getPrekey: User: %s does not have client: %s", userId, clientId);
                return Response
                        .ok(new ErrorMessage("Unknown client"))
                        .status(404)
                        .build();
            }

            PreKey preKey = prekeysDAO.get(clientId);

            if (preKey == null) {
                Logger.error("PrekeysResource.getPrekey: No prekeys for client: %s", clientId);
                return Response
                        .ok(new ErrorMessage("Prekeys missing"))
                        .status(404)
                        .build();
            }

            Device device = clientsDAO.getDevice(userId, clientId);
            if (device == null) {
                Logger.error("PrekeysResource.getPrekey: User: %s does not have client: %s", userId, clientId);
                return Response
                        .ok(new ErrorMessage("User does not have this client"))
                        .status(500)
                        .build();
            }

            if (preKey.id != device.lastKey) {
                prekeysDAO.mark(clientId, preKey.id);
            }

            ClientPrekey result = new ClientPrekey();
            result.client = clientId;
            result.prekey = preKey;

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PrekeysResource.getPrekey : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("{userId}/prekeys")
    @ApiOperation(value = "Get prekey for user and all its clients")
    @Authorization("Bearer")
    public Response getPrekeys(@PathParam("userId") UUID userId) {

        try {
            _Result result = new _Result();
            result.user = userId;

            PrekeysDAO prekeysDAO = jdbi.onDemand(PrekeysDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            List<String> clients = clientsDAO.getClients(userId);
            for (String clientId : clients) {
                Device device = clientsDAO.getDevice(userId, clientId);
                if (device == null) {
                    Logger.error("PrekeysResource.getPrekeys: User: %s does not have client: %s", userId, clientId);
                    continue;
                }

                PreKey preKey = prekeysDAO.get(clientId);
                if (preKey == null) {
                    Logger.error("PrekeysResource.getPrekeys: No prekeys for client: %s", clientId);
                    continue;
                }

                if (preKey.id != device.lastKey) {
                    prekeysDAO.mark(clientId, preKey.id);
                }

                ClientPrekey clientKey = new ClientPrekey();
                clientKey.client = clientId;
                clientKey.prekey = preKey;

                result.clients.add(clientKey);
            }

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PrekeysResource.getPrekeys : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("{userId}/clients")
    @ApiOperation(value = "Get all devices for the user")
    @Authorization("Bearer")
    public Response getClients(@PathParam("userId") UUID userId) {
        try {
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            List<Device> devices = clientsDAO.getDevices(userId);

            return Response.
                    ok(devices).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PrekeysResource.getClients : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    public static class ClientPrekey {
        public PreKey prekey;
        public String client;
    }

    public static class _Result {
        public UUID user;
        public ArrayList<ClientPrekey> clients = new ArrayList<>();
    }
}
