package com.aves.server.resource;

import com.aves.server.Aves;
import com.aves.server.Limiter;
import com.aves.server.model.AssetKey;
import com.aves.server.model.ErrorMessage;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.minio.errors.ErrorResponseException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.aves.server.tools.Util.*;

@Api
@Path("/assets/v3")
@Produces(MediaType.APPLICATION_JSON)
public class AssetsResource {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @POST
    @ApiOperation(value = "Store asset into S3")
    @Authorization("Bearer")
    public Response post(@HeaderParam("Content-type") String contentType,
                         @ApiParam InputStream data) {
        try {
            ByteArrayDataSource ds = new ByteArrayDataSource(data, contentType);
            MimeMultipart mimeMultipart = new MimeMultipart(ds);
            BodyPart bodyPart1 = mimeMultipart.getBodyPart(0);
            BodyPart bodyPart2 = mimeMultipart.getBodyPart(1);

            String contentMD5 = bodyPart2.getHeader("Content-MD5")[0];
            byte[] bytes = toByteArray(bodyPart2.getInputStream());

            String challenge = calcMd5(bytes);
            if (!Objects.equals(contentMD5, challenge)) {
                return Response
                        .ok(new ErrorMessage("MD5 is incorrect"))
                        .status(400)
                        .build();
            }

            AssetKey assetKey = new AssetKey();
            assetKey.key = s3UploadFile(bytes);

            Metadata metadata = objectMapper.readValue(bodyPart1.getInputStream(), Metadata.class);
            if (!metadata.visible) {
                Date exp = new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(30));
                assetKey.token = Jwts.builder()
                        .setIssuer("https://aves.services.zinfra.io")
                        .setExpiration(exp)
                        .signWith(Aves.getKey())
                        .compact();
                assetKey.expires = time();
            }

            return Response.
                    ok(assetKey).
                    status(201).
                    build();
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.error("AssetsResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("/{assetId}")
    @ApiOperation(value = "Fetch asset from S3")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context, @PathParam("assetId") String assetId) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            if (Limiter.rate("/assets", userId, 200)) {
                return Response.
                        ok(new ErrorMessage("Hold your horses!", 429, "limit-reached")).
                        status(429).
                        build();
            }

            InputStream object = s3DownloadFile(assetId);
            return Response.
                    ok(object).
                    build();
        } catch (ErrorResponseException e) {
            Logger.warning("AssetsResource.get : %s", e.errorResponse().code());
            // Logger.debug("AssetsResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.errorResponse().code()))
                    .status(404)
                    .build();
//            return Response
//                    .ok(getErrorImage())
//                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("AssetsResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("public")
        public boolean visible;
        @JsonProperty("retention")
        public String retention;
    }
}
