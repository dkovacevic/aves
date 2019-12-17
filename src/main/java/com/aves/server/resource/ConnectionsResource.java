package com.aves.server.resource;

import com.aves.server.DAO.ConnectionsDAO;
import com.aves.server.model.Connection;
import com.aves.server.tools.Util;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Api
@Path("/connections")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionsResource {
    private final ConnectionsDAO connectionsDAO;

    public ConnectionsResource(DBI jdbi) {
        connectionsDAO = jdbi.onDemand(ConnectionsDAO.class);
    }

    @GET
    @ApiOperation(value = "Get user's connections")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context) {
        UserConnectionList result = new UserConnectionList();

        UUID userId = (UUID) context.getProperty("zuid");

        List<UUID> connections = connectionsDAO.getConnections(userId);
        for (UUID to : connections) {
            Connection connection = new Connection();
            connection.time = Util.time();
            connection.from = userId;
            connection.to = to;

            result.connections.add(connection);
        }
        return Response.
                ok(result).
                build();
    }

    static class UserConnectionList {
        @JsonProperty("has_more")
        public boolean more;
        @JsonProperty
        public ArrayList<Connection> connections = new ArrayList<>();
    }

}
