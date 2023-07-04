package com.aves.server.resource;

import afu.org.checkerframework.checker.igj.qual.I;
import com.aves.server.Aves;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/api-version")
@Produces(MediaType.APPLICATION_JSON)
public class ApiVersionResource {
    @GET
    @ApiOperation(value = "Get API Version", response = _Versions.class)
    public Response getConfig() {
        return Response.
                ok(new _Versions()).
                build();
    }

    public static class _Versions {
        public Integer[] development = new Integer[]{1};
        public String domain = "aves.com";
        public boolean federation = false;
        public Integer[] supported = new Integer[]{1};
    }
}
