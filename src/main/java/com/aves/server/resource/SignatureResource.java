package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.Limiter;
import com.aves.server.clients.SwisscomClient;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
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

            if (Limiter.rate("/signature", signer, 2)) {
                return Response.
                        ok(new ErrorMessage("Hold your horses!", 403, "signature-limit-reached")).
                        status(403).
                        build();
            }

            User user = userDAO.getUser(signer);
            String hash = request.hash;
            String documentId = request.documentId;
            String name = request.name;

            SwisscomClient.RootSignResponse res = swisscomClient.sign(user, documentId, name, hash);
            if (res.isError()) {
                Logger.warning("SignatureResource.request: major: %s, error: %s",
                        res.getMajor(),
                        res.getErrorMessage());
                return Response.
                        ok(new ErrorMessage(res.getErrorMessage(), 400, res.getMajor())).
                        status(400).
                        build();
            }

            SwisscomClient.SignResponse signResponse = res.signResponse;
            SwisscomClient.OptionalOutputs optionalOutputs = signResponse.optionalOutputs;

            // try /pending to see if the DN was parsed correctly
            SwisscomClient.RootSignResponse pending = swisscomClient.pending(optionalOutputs.responseId);

            if (pending.isError()) {
                Logger.warning("SignatureResource.request: UserId: %s, Major: %s Error: %s",
                        user.id,
                        pending.getMajor(),
                        pending.getErrorMessage());
                return Response.
                        ok(new ErrorMessage(pending.getErrorMessage(), 400, pending.getMajor())).
                        status(400).
                        build();
            }

            SignResponse result = new SignResponse();
            result.responseId = optionalOutputs.responseId;
            result.consentURL = optionalOutputs.stepUpAuthorisationInfo.result.url;

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            Logger.error("SignatureResource.request : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(400)
                    .build();
        }
    }

    @GET
    @Path("pending/{responseId}")
    @ApiOperation(value = "Try to fetch the signature", response = Signature.class)
    @Authorization("Bearer")
    public Response pending(@Context ContainerRequestContext context, @PathParam("responseId") UUID responseId) {
        try {
            Thread.sleep(2000); // forgive me Father, for I have sinned

            final SwisscomClient.RootSignResponse res = swisscomClient.pending(responseId);
            if (res.isError()) {
                Logger.warning("SignatureResource.pending: ResponseId: %s, Major: %s, Error: %s",
                        responseId,
                        res.getMajor(),
                        res.getErrorMessage());
                return Response.
                        ok(new ErrorMessage(res.getErrorMessage(), 400, res.getMajor())).
                        status(400).
                        build();
            }

            SwisscomClient.SignResponse signResponse = res.signResponse;
            if (signResponse.isPending()) {
                return Response.
                        ok(new ErrorMessage("Epstein didn't kill himself", 503, signResponse.getMajor())).
                        status(503).
                        build();
            }

            SwisscomClient.ExtendedSignatureObject signatureObject = signResponse.signature.other.signatureObjects.extendedSignatureObject;

            Signature signature = new Signature();
            signature.documentId = signatureObject.documentId;
            signature.cms = signatureObject.base64Signature.value;
            signature.serialNumber = signResponse.optionalOutputs.stepUpAuthorisationInfo.result.serialNumber;

            return Response.
                    ok(signature).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SignatureResource.pending : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(400)
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
        public String documentId;
        @NotNull
        public String name;
        @NotNull
        public String hash;
    }

    public static class Signature {
        @NotNull
        public String documentId;
        @NotNull
        public String cms;
        @NotNull
        public String serialNumber;
    }
}
