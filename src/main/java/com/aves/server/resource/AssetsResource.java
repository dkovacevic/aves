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
import io.minio.errors.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.xmlpull.v1.XmlPullParserException;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.aves.server.tools.Util.*;

@Api
@Path("/assets/v3")
public class AssetsResource {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @POST
    @ApiOperation(value = "Store asset into S3")
    @Authorization("Bearer")
    @Produces(MediaType.APPLICATION_JSON)
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
                assetKey.expires = time(exp);
            }

            return Response.
                    ok(assetKey).
                    status(201).
                    build();
        } catch (Exception e) {
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
    public Response get(@Context ContainerRequestContext context,
                        @HeaderParam("Asset-Token") String assetToken,
                        @PathParam("assetId") String assetId) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");
            if (Limiter.rate("/assets", userId, 100)) {
                return Response
                        .ok(new ErrorMessage("Hold your horses!", 429, "limit-reached"))
                        .header("content-type", MediaType.APPLICATION_OCTET_STREAM)
                        .status(429)
                        .build();
            }
            return Response
                    .ok(s3DownloadFile(assetId))
                    .header("content-type", MediaType.APPLICATION_OCTET_STREAM)
                    .build();
        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException |
                InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException |
                InternalException | InvalidArgumentException e) {
            Logger.warning("AssetsResource.get : %s", "Asset not found: " + assetId);
            return Response
                    .ok(new ErrorMessage("Asset not found", 404, "not-found"))
                    .header("content-type", MediaType.APPLICATION_JSON)
                    .status(404)
                    .build();
        } catch (Exception e) {
            Logger.error("AssetsResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage(), 400, "server-error"))
                    .header("content-type", MediaType.APPLICATION_JSON)
                    .status(400)
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
