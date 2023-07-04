package com.aves.server.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/feature-configs")
@Produces(MediaType.APPLICATION_JSON)
public class FeaturesConfigResource {
    @GET
    @ApiOperation(value = "Get Feature Configs", response = _Config.class)
    public Response getConfig() {
        return Response.
                ok(new _Config()).
                build();
    }

    public static class _Config {
    }
}
