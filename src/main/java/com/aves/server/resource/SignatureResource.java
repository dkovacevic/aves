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
import org.jdbi.v3.core.Jdbi;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Api
@Path("/signature")
@Produces(MediaType.APPLICATION_JSON)
public class SignatureResource {
    private final UserDAO userDAO;
    private final SwisscomClient swisscomClient;
    private final ConcurrentHashMap<UUID, Date> rates = new ConcurrentHashMap<>();

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

            if (rateLimit(signer)) {
                Logger.warning("SignatureResource.request: rate limiting user: %s", signer);
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
                Logger.warning("SignatureResource.request: %s", res.getErrorMessage());
                return Response.
                        ok(new ErrorMessage(res.getErrorMessage(), 400, "signature-error")).
                        status(400).
                        build();
            }

            SwisscomClient.SignResponse signResponse = res.signResponse;
            SwisscomClient.OptionalOutputs optionalOutputs = signResponse.optionalOutputs;

            // try /pending to see if the DN was parsed correctly
            SwisscomClient.RootSignResponse pending = swisscomClient.pending(optionalOutputs.responseId);

            if (pending.isError()) {
                Logger.warning("SignatureResource.request: UserId: %s, Error: %s",
                        user.id,
                        pending.getErrorMessage());
                return Response.
                        ok(new ErrorMessage(pending.getErrorMessage(), 400, "signature-error")).
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
            final SwisscomClient.RootSignResponse res = swisscomClient.pending(responseId);
            if (res.isError()) {
                Logger.warning("SignatureResource.pending: ResponseId: %s, Error: %s",
                        responseId,
                        res.getErrorMessage());
                return Response.
                        ok(new ErrorMessage(res.getErrorMessage(), 404, "signature-unknown-responseId")).
                        status(404).
                        build();
            }

            SwisscomClient.SignResponse signResponse = res.signResponse;
            if (signResponse.isPending()) {
                Thread.sleep(1000); // Forgive me
                return Response.
                        ok(new ErrorMessage("Signature is still pending", 503, "signature-pending")).
                        status(503).
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
                    .status(400)
                    .build();
        }
    }

    private boolean rateLimit(UUID signer) {
        final Date now = new Date();
        final Date date = rates.computeIfAbsent(signer, x -> now);
        final long elapsed = now.getTime() - date.getTime();
        if (elapsed == 0) {
            return false;
        }
        if (elapsed > TimeUnit.SECONDS.toMillis(60)) {
            rates.remove(signer);
            return false;
        }

        return true;
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
    }
}
