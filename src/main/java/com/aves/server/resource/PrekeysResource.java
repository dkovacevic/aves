package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.Logger;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.otr.Missing;
import com.aves.server.model.otr.PreKey;
import com.aves.server.model.otr.PreKeys;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Api
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class PrekeysResource {
    private static final int MAX_PREKEY_ID = 0xFFFE;
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

            PreKeys preKeys = new PreKeys();

            for (UUID id : missing.keySet()) {
                HashMap<String, PreKey> map = new HashMap<>();
                for (String client : missing.get(id)) {
                    PreKey preKey = prekeysDAO.get(client);
                    map.put(client, preKey);

                    if (preKey.id != MAX_PREKEY_ID) {
                        prekeysDAO.mark(client, preKey.id);
                    }
                }
                preKeys.put(id, map);
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
            PreKey preKey = prekeysDAO.get(clientId);
            if (preKey.id != MAX_PREKEY_ID) {
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
                PreKey preKey = prekeysDAO.get(clientId);
                if (preKey.id != MAX_PREKEY_ID) {
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

    public static class ClientPrekey {
        public PreKey prekey;
        public String client;
    }

    public static class _Result {
        public UUID user;
        public ArrayList<ClientPrekey> clients = new ArrayList<>();
    }
}
