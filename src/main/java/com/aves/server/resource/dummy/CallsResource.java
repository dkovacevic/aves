package com.aves.server.resource.dummy;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

@Api
@Path("/calls/config/v2")
@Produces(MediaType.APPLICATION_JSON)
public class CallsResource {
    @GET
    @ApiOperation(value = "Get calling config")
    @Authorization("Bearer")
    public Response get() {
        return Response.ok(new _Result()).build();
    }

    public static class _Result {
        public HashMap<String, Object> settings = new HashMap<>();
    }
}
