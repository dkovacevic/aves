package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.clients.SwisscomClient;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
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
            final SignResponse dummySigResponse = getDummySigResponse();
            if (dummySigResponse != null) {
                return Response.
                        ok(dummySigResponse).
                        build();
            }

            UUID signer = (UUID) context.getProperty("zuid");

            if (rateLimit(signer))
                return Response.
                        ok(new ErrorMessage("Hold your horses!")).
                        status(429).
                        build();

            User user = userDAO.getUser(signer);
            String hash = request.hash;
            String documentId = request.documentId;
            String name = request.name;

            SwisscomClient.SignResponse signResponse = swisscomClient.sign(user, documentId, name, hash);

            final SwisscomClient.OptionalOutputs optionalOutputs = signResponse.optionalOutputs;

            if (optionalOutputs == null) {
                return Response.
                        ok(new ErrorMessage(signResponse.result.minor)).
                        status(400).
                        build();
            }

            SignResponse result = new SignResponse();
            result.responseId = optionalOutputs.responseId;
            if (optionalOutputs.stepUpAuthorisationInfo != null)
                result.consentURL = optionalOutputs.stepUpAuthorisationInfo.result.url;

            return Response.
                    ok(result).
                    build();
        } catch (Exception e) {
            //e.printStackTrace();
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
            Signature dummy = getDummySignature();

            if (dummy != null) {
                return Response.
                        ok(dummy).
                        build();
            }
            
            SwisscomClient.SignResponse signResponse = swisscomClient.pending(responseId);

            if (signResponse == null || signResponse.signature == null) {
                return Response.
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
            //e.printStackTrace();
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

    @Nullable
    private Signature getDummySignature() throws IOException {
        final String documentId = "a4ede2b6-396c-4f71-894c-af4a25a91c52";
        final String cmdFilename = String.format("signatures/%s.cms", documentId);
        InputStream cms = SignatureResource
                .class
                .getClassLoader()
                .getResourceAsStream(cmdFilename);

        if (cms != null) {
            final byte[] bytes = Util.toByteArray(cms);
            Signature dummy = new Signature();
            dummy.documentId = documentId;
            dummy.cms = Base64.getEncoder().encodeToString(bytes);
            return dummy;
        }

        return null;
    }

    @Nullable
    private SignResponse getDummySigResponse() {
        SignResponse result = new SignResponse();
        result.responseId = UUID.fromString("5cdcc7bb-6f9e-44f2-b524-cdd865657729");
        result.consentURL = "https://ais-sas.swisscom.com/sas/web/tk8f63384b81fd4efeb3f278b86506a65etx/otp?lang=en-us";
        return null;
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
