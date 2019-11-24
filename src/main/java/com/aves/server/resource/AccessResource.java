package com.aves.server.resource;

import com.aves.server.Logger;
import com.aves.server.Server;
import com.aves.server.model.AccessToken;
import com.aves.server.model.Configuration;
import com.aves.server.model.ErrorMessage;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Api
@Path("/access")
@Produces(MediaType.APPLICATION_JSON)
public class AccessResource {
    private final DBI jdbi;
    private final Configuration config;

    public AccessResource(DBI jdbi, Configuration config) {
        this.jdbi = jdbi;
        this.config = config;
    }

    @POST
    @ApiOperation(value = "Obtain an access tokens for a cookie")
    public Response post(@QueryParam("access_token") String access,
                         @HeaderParam("Authorization") String header,
                         @CookieParam("zuid") String cookie) {
        try {
            if (header == null)
                header = access;

            String expired = cookie != null ? cookie : header;
            UUID userId;
            try {
                String subject = Jwts.parser()
                        .setSigningKey(Server.getKey())
                        .parseClaimsJws(expired)
                        .getBody()
                        .getSubject();

                userId = UUID.fromString(subject);
            } catch (ExpiredJwtException e) {
                userId = UUID.fromString(e.getClaims().getSubject());
            }

            long mills = TimeUnit.SECONDS.toMillis(config.tokenExpiration);
            Date exp = new Date(new Date().getTime() + mills);

            String token = Jwts.builder()
                    .setIssuer("https://aves.com")
                    .setSubject("" + userId)
                    .setExpiration(exp)
                    .signWith(Server.getKey())
                    .compact();

            AccessToken result = new AccessToken();
            result.expiresIn = config.tokenExpiration;
            result.accessToken = token;
            result.tokenType = "Bearer";
            result.user = userId;

            String authorization = String.format("%s %s", result.tokenType, result.accessToken);

            return Response.
                    ok(result).
                    cookie(new NewCookie("zuid", authorization)).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("AccessResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}