package com.aves.server.resource.dummy;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/onboarding/v3")
@Produces(MediaType.APPLICATION_JSON)
public class OnboardingResource {
    @POST
    @ApiOperation(value = "Addressbook")
    @Authorization("Bearer")
    public Response post() {
        return Response.ok().build();
    }
}
