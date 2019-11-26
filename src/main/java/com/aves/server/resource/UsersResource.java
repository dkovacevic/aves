package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.Logger;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    @ApiOperation(value = "Register new user")
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
}
