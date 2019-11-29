package com.aves.server.healthchecks;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/healthcheck")
public class StatusHealthcheck {
    @GET
    public Response get() {
        return Response.ok().build();
    }
}
