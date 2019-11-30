package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

@Api
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource {
    private final DBI jdbi;

    public UsersResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @GET
    @Path("{userId}")
    @ApiOperation(value = "Get user's details")
    @Authorization("Bearer")
    public Response get(@PathParam("userId") UUID userId) {
        try {
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
            Logger.error("UsersResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @ApiOperation(value = "Get users by userId")
    @Authorization("Bearer")
    public Response getUsers(@ApiParam("List of userIds as UUID strings") @QueryParam("ids") String users) {
        try {
            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            ArrayList<User> result = new ArrayList<>();
            for (String id : users.split(",")) {
                UUID userId = UUID.fromString(id.trim());
                User user = userDAO.getUser(userId);
                if (user != null)
                    result.add(user);
            }
            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("UsersResource.getUsers : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
