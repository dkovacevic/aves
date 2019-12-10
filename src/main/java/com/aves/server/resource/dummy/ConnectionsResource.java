package com.aves.server.resource.dummy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Api
@Path("/connections")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionsResource {
    @GET
    @ApiOperation(value = "Get user's connections")
    @Authorization("Bearer")
    public Response get() {
        return Response.
                ok(new UserConnectionList()).
                build();
    }

    static class UserConnectionList {
        @JsonProperty("has_more")
        public boolean more;
        @JsonProperty
        public ArrayList<String> connections = new ArrayList<>();
    }
}
