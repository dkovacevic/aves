package com.aves.server.resource;

import com.aves.server.Aves;
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

    public static class _Config {
        public String title = "AVES Staging";
        public _Endpoints endpoints = new _Endpoints();
    }

    public static class _Endpoints {
        public String backendURL = "https://" + Aves.config.domain;
        public String backendWSURL = "https://" + Aves.config.domain;
        public String accountsURL = "https://" + Aves.config.domain;
        public String teamsURL = "https://" + Aves.config.domain;
        public String websiteURL = "https://" + Aves.config.domain;
        public String blackListURL = "https://clientblacklist.wire.com/prod/android";
    }
}
