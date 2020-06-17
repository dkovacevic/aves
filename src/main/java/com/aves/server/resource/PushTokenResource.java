package com.aves.server.resource;

import com.aves.server.DAO.PushTokensDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.PushToken;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Api
@Path("/push/tokens")
@Produces(MediaType.APPLICATION_JSON)
public class PushTokenResource {
    private final PushTokensDAO pushTokensDAO;

    public PushTokenResource(PushTokensDAO pushTokensDAO) {
        this.pushTokensDAO = pushTokensDAO;
    }

    @POST
    @ApiOperation(value = "Register new push token")
    @Authorization("Bearer")
    public Response post(@Context ContainerRequestContext context,
                         @ApiParam @Valid PushToken pushToken) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            pushTokensDAO.insert(pushToken.token, userId, pushToken.client, pushToken.app, pushToken.transport);

            return Response.
                    ok(pushToken).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PushTokenResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @ApiOperation(value = "Get all push tokens")
    @Authorization("Bearer")
    public Response getAll(@Context ContainerRequestContext context) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            _Result result = new _Result();
            result.tokens = pushTokensDAO.getPushTokens(userId);

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PushTokenResource.getAll : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @DELETE
    @Path("{pid}")
    @ApiOperation(value = "Delete push token")
    @Authorization("Bearer")
    public Response delete(@Context ContainerRequestContext context,
                           @PathParam("pid") String pid) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            final int delete = pushTokensDAO.delete(pid, userId);

            return Response.
                    ok().
                    status(delete > 0 ? 204 : 404).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PushTokenResource.delete : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    static class _Result {
        List<PushToken> tokens;
    }
}
