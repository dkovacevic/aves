package com.aves.server.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {
    @GET
    @ApiOperation(value = "Get Config")
    public Response getConfig() {
        return Response.
                ok(new _Config()).
                build();
    }

    static class _Config {
        public String title = "AVES Staging";
        public _Endpoints endpoints = new _Endpoints();
    }

    static class _Endpoints {
        public String backendURL = "https://aves.services.zinfra.io";
        public String backendWSURL = "https://aves.services.zinfra.io";
        public String accountsURL = "https://aves.services.zinfra.io";
        public String teamsURL = "https://aves.services.zinfra.io";
        public String websiteURL = "https://aves.services.zinfra.io";
        public String blackListURL = "https://clientblacklist.wire.com/prod/android";
    }
}
