package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.Logger;
import com.aves.server.clients.SwisscomClient;
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
import javax.ws.rs.*;
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
    private final SwisscomClient swisscomClient;

    public SignatureResource(DBI jdbi, SwisscomClient swisscomClient) {
        this.jdbi = jdbi;
        this.swisscomClient = swisscomClient;
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

            SwisscomClient.SignResponse signResponse = swisscomClient.sign(user, documentId, hash);

            SignResponse result = new SignResponse();
            result.consentURL = signResponse.optionalOutputs.stepUpAuthorisationInfo.result.url;
            result.responseId = signResponse.optionalOutputs.responseId;

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SignatureResource.request : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("pending/{responseId}")
    @ApiOperation(value = "Try to fetch the signature")
    @Authorization("Bearer")
    public Response pending(@PathParam("responseId") UUID responseId) {
        try {

            SwisscomClient.SignResponse signResponse = swisscomClient.pending(responseId);

            if (signResponse == null || signResponse.signature == null) {
                return Response.
                        status(412).
                        build();
            }

            Signature signature = new Signature();

            SwisscomClient.ExtendedSignatureObject signatureObject = signResponse.signature.other.signatureObjects.extendedSignatureObject;
            signature.documentId = signatureObject.documentId;
            signature.cms = signatureObject.base64Signature.value;

            return Response.
                    ok(signature).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SignatureResource.pending : %s", e);
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
