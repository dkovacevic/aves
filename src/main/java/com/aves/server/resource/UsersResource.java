package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

@Api
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource {
    private final UserDAO userDAO;

    public UsersResource(Jdbi jdbi) {
        this.userDAO = jdbi.onDemand(UserDAO.class);
    }

    @GET
    @Path("{userId}")
    @ApiOperation(value = "Get user's details")
    @Authorization("Bearer")
    public Response get(@PathParam("userId") UUID userId) {
        try {
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
    @Path("handles/{handle}")
    @ApiOperation(value = "Get information on a user handle")
    @Authorization("Bearer")
    public Response handle(@PathParam("handle") String handle) {
        return Response.
                status(404).
                build();
    }

    @GET
    @ApiOperation(value = "Get users by userId")
    @Authorization("Bearer")
    public Response getUsers(@ApiParam("List of userIds as UUID strings") @QueryParam("ids") String users) {
        try {
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

    @POST
    @Path("handles")
    @ApiOperation(value = "Check availability of user handles")
    @Authorization("Bearer")
    public Response handles(@Context ContainerRequestContext context,
                            @ApiParam CheckHandles checkHandles) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            User user = userDAO.getUser(userId);

            checkHandles.handles.remove(user.getHandle());

            return Response.
                    ok(checkHandles.handles).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("UsersResource.handles : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("{userId}/rich-info")
    @ApiOperation(value = "Get user's rich info'")
    @Authorization("Bearer")
    public Response getRichInfo(@PathParam("userId") UUID userId) {
        User user = userDAO.getUser(userId);

        RichInfo richInfo = new RichInfo();

        RichField field = new RichField();
        field.type = "Firstname";
        field.value = user.firstname;
        richInfo.fields.add(field);

        field = new RichField();
        field.type = "Lastname";
        field.value = user.lastname;
        richInfo.fields.add(field);

        field = new RichField();
        field.type = "Country";
        field.value = user.country;
        richInfo.fields.add(field);

        field = new RichField();
        field.type = "Phone";
        field.value = user.phone;
        richInfo.fields.add(field);

        field = new RichField();
        field.type = "Email";
        field.value = user.email;
        richInfo.fields.add(field);

        return Response.
                ok(richInfo).
                build();
    }

    static class CheckHandles {
        @JsonProperty("return")
        public int ret;
        public ArrayList<String> handles;
    }

    static class RichInfo {
        @JsonProperty
        public int version = 1;
        public ArrayList<RichField> fields = new ArrayList<>();
    }

    static class RichField {
        public String value;
        public String type;
    }
}
