package com.aves.server.resource;

import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.otr.AssetKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeBodyPart;
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeMultipart;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.glassfish.jersey.message.internal.DataSourceProvider;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Api
@Path("/assets/v3")
@Produces(MediaType.APPLICATION_JSON)
public class AssetsResource {
    private final DBI jdbi;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MinioClient minioClient;
    private final String BUCKET_NAME = "aves-bucket";

    public AssetsResource(DBI jdbi) throws InvalidPortException, InvalidEndpointException {
        this.jdbi = jdbi;
        this.minioClient = new MinioClient("http://play.min.io", "Q3AM3UQ867SPQQA43P2F",
                "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
    }

    static String calcMd5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes, 0, bytes.length);
        byte[] hash = md.digest();
        byte[] byteArray = Base64.getEncoder().encode(hash);
        return new String(byteArray);
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[1024 * 4];
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }

    @POST
    @ApiOperation(value = "Store asset into S3")
    @Authorization("Bearer")
    public Response post(@HeaderParam("Content-type") String contentType,
                         @ApiParam InputStream data) {
        try {
            DataSourceProvider.ByteArrayDataSource ds = new DataSourceProvider.ByteArrayDataSource(data, contentType);
            MimeMultipart mimeMultipart = new MimeMultipart(ds, new ContentType(contentType));
            MimeBodyPart bodyPart1 = mimeMultipart.getBodyPart(0);
            MimeBodyPart bodyPart2 = mimeMultipart.getBodyPart(1);


            String contentMD5 = bodyPart2.getContentMD5();
            byte[] bytes = toByteArray(bodyPart2.getInputStream());

            String challenge = calcMd5(bytes);

            if (!Objects.equals(contentMD5, challenge)) {
                return Response
                        .ok(new ErrorMessage("MD5 is incorrect"))
                        .status(400)
                        .build();
            }

            if (!minioClient.bucketExists(BUCKET_NAME)) {
                minioClient.makeBucket(BUCKET_NAME);
            }

            AssetKey assetKey = new AssetKey();

            assetKey.key = UUID.randomUUID();

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(
                        BUCKET_NAME,
                        assetKey.key.toString(),
                        bais,
                        (long) bodyPart2.getSize(),
                        "application/octet-stream");
            }

            _Metadata metadata = objectMapper.readValue(bodyPart1.getInputStream(), _Metadata.class);
            if (!metadata.visible) {
                Date exp = new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(30));
                assetKey.token = Jwts.builder()
                        .setIssuer("https://aves.com")
                        .setExpiration(exp)
                        .signWith(Server.getKey())
                        .compact();
                assetKey.expires = data.toString();
            }

            return Response.
                    ok(assetKey).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
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
    //@Authorization("Bearer")
    public Response get(@PathParam("assetId") UUID assertId,
                        @HeaderParam("Asset-Token") String token) {

        try {
            if (token != null) {
                //todo check the token against db
                Claims body = Jwts.parser()
                        .setSigningKey(Server.getKey())
                        .parseClaimsJws(token)
                        .getBody();
            }

            InputStream object = minioClient.getObject(BUCKET_NAME, assertId.toString());

            return Response.
                    ok(object).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("AssetsResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    public static class _Metadata {
        @JsonProperty("public")
        public boolean visible;
        @JsonProperty("retention")
        public String retention;
    }
}
