package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
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
import java.util.UUID;

@Api
@Path("/self")
@Produces(MediaType.APPLICATION_JSON)
public class SelfResource {
    private final DBI jdbi;

    public SelfResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @GET
    @ApiOperation(value = "Get your details")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            User user = userDAO.getUser(userId);

            if (user == null) {
                return Response.
                        status(404).
                        build();
            }

            return Response.
                    ok(user).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SelfResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
