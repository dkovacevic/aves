package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.clients.SwisscomClient;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.hibernate.validator.constraints.NotEmpty;
import org.jdbi.v3.core.Jdbi;

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
    private final UserDAO userDAO;
    private final SwisscomClient swisscomClient;

    public SignatureResource(Jdbi jdbi, SwisscomClient swisscomClient) {
        this.userDAO = jdbi.onDemand(UserDAO.class);
        this.swisscomClient = swisscomClient;
    }

    @POST
    @Path("request")
    @ApiOperation(value = "Send Signature request", response = SignResponse.class)
    @Authorization("Bearer")
    public Response request(@Context ContainerRequestContext context, @ApiParam @Valid SignRequest request) {
        try {
            UUID signer = (UUID) context.getProperty("zuid");

            User user = userDAO.getUser(signer);
            String hash = request.hash;
            UUID documentId = request.documentId;

            SwisscomClient.SignResponse signResponse = swisscomClient.sign(user, documentId, hash);

            final SwisscomClient.OptionalOutputs optionalOutputs = signResponse.optionalOutputs;

            SignResponse result = new SignResponse();
            result.responseId = optionalOutputs.responseId;
            if (optionalOutputs.stepUpAuthorisationInfo != null)
                result.consentURL = optionalOutputs.stepUpAuthorisationInfo.result.url;

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
    @ApiOperation(value = "Try to fetch the signature", response = Signature.class)
    @Authorization("Bearer")
    public Response pending(@PathParam("responseId") UUID responseId) {
        try {
            SwisscomClient.SignResponse signResponse = swisscomClient.pending(responseId);

            if (signResponse == null || signResponse.signature == null) {
                return Response.
                        status(412).
                        build();
            }

            SwisscomClient.ExtendedSignatureObject signatureObject = signResponse.signature.other.signatureObjects.extendedSignatureObject;

            Signature signature = new Signature();
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
