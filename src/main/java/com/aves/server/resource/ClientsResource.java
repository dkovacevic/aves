package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.PrekeysDAO;
import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.NewClient;
import com.aves.server.model.PreKey;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.*;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

@Api
@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
public class ClientsResource {
    private final SecureRandom random = new SecureRandom();

    public String next() {
        return new BigInteger(130, random).toString(32);
    }

    public String next(int length) {
        return next().substring(0, length);
    }

    private final DBI jdbi;

    public ClientsResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Register new device")
    public Response client(@ApiParam(hidden = true) @NotNull @HeaderParam("Authorization") String cookie,
                           @ApiParam @Valid NewClient newClient) {

        try {
            String[] split = cookie.split(" ");
            String token = split[1];

            String subject = Jwts.parser()
                    .setSigningKey(Server.getKey())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);
            String clientId = next(6);

            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);
            PrekeysDAO prekeysDAO = jdbi.onDemand(PrekeysDAO.class);

            PreKey lastkey = newClient.lastkey;
            clientsDAO.insert(clientId, userId, lastkey.id);

            for (PreKey preKey : newClient.prekeys) {
                prekeysDAO.insert(clientId, preKey.id, preKey.key);
            }
            prekeysDAO.insert(clientId, lastkey.id, lastkey.key);

            NewClient result = new NewClient();
            result.id = clientId;

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ClientResource.client : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
