package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.Logger;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.hibernate.validator.constraints.NotEmpty;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Api
@Path("/signature")
@Produces(MediaType.APPLICATION_JSON)
public class SignatureResource {
    private final DBI jdbi;

    public SignatureResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @Path("request")
    @ApiOperation(value = "Send Signature request")
    @Authorization("Bearer")
    public Response request(@Context ContainerRequestContext context,
                            @ApiParam @Valid SignRequest request) {
        try {
            UUID signee = (UUID) context.getProperty("zuid");

            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            User user = userDAO.getUser(signee);
            String hash = request.hash;
            UUID documentId = request.documentId;

            SignResponse result = new SignResponse();
            result.consentURL = "https://ais-sas.swisscom.com/sas/web/tk1960ca09411f495791814c7af3cfdbd0tx/otp?lang=en";
            result.responseId = UUID.randomUUID();

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SignatureResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @POST
    @Path("pending/{responseId}")
    @ApiOperation(value = "Try to fetch the signature")
    @Authorization("Bearer")
    public Response pending(@PathParam("responseId") UUID responseId) {
        try {
            Signature signature = new Signature();
            signature.documentId = UUID.randomUUID();
            signature.cms = "Base64 encoded signature";

            return Response.
                    ok(signature).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SignatureResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    public static class SignResponse {
        @NotNull
        public String consentURL;
        @NotNull
        public UUID responseId;
    }

    public static class SignRequest {
        @NotNull
        public UUID documentId;
        @NotEmpty
        @NotNull
        public String hash;
    }

    public static class Signature {
        @NotNull
        public UUID documentId;
        @NotNull
        public String cms;
    }
}
