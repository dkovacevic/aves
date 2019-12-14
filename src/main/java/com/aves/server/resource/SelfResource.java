package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lambdaworks.crypto.SCryptUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.hibernate.validator.constraints.Length;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Api
@Path("/self")
@Produces(MediaType.APPLICATION_JSON)
public class SelfResource {
    private final UserDAO userDAO;

    public SelfResource(DBI jdbi) {
        userDAO = jdbi.onDemand(UserDAO.class);
    }

    @GET
    @ApiOperation(value = "Get your details")
    @Authorization("Bearer")
    public Response getSelf(@Context ContainerRequestContext context) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");
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
            Logger.error("SelfResource.getSelf : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @HEAD
    @Path("password")
    @ApiOperation(value = "Checks for password")
    @Authorization("Bearer")
    public Response checkPassword(@Context ContainerRequestContext context) {
        UUID userId = (UUID) context.getProperty("zuid");
        Boolean reset = userDAO.getResetPassword(userId);
        return Response.
                status(reset ? 404 : 200).
                build();
    }

    @PUT
    @Path("password")
    @ApiOperation(value = "Update password")
    @Authorization("Bearer")
    public Response updatePassword(@Context ContainerRequestContext context,
                                   @Valid NewPassword newPassword) {
        UUID userId = (UUID) context.getProperty("zuid");

        if (newPassword.oldPassword != null) {
            String hashed = userDAO.getHash(userId);
            if (hashed == null || !SCryptUtil.check(newPassword.oldPassword, hashed)) {
                return Response
                        .ok(new ErrorMessage("Old password does not match"))
                        .status(403)
                        .build();
            }
        }

        String hash = SCryptUtil.scrypt(newPassword.newPassword, 16384, 8, 1);
        int updateHash = userDAO.updateHash(userId, hash);

        return Response.
                ok().
                build();
    }

    @HEAD
    @Path("consent")
    @ApiOperation(value = "Checks for consent")
    @Authorization("Bearer")
    public Response getConsent() {
        return Response.
                ok().
                build();
    }

    static class NewPassword {
        @JsonProperty("new_password")
        @NotNull
        @Length(min = 6, max = 1024)
        public String newPassword;

        @JsonProperty("old_password")
        public String oldPassword;
    }
}
