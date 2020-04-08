package com.aves.server.resource;

import com.aves.server.Aves;
import com.aves.server.model.AccessToken;
import com.aves.server.model.Configuration;
import com.aves.server.model.ErrorMessage;
import com.aves.server.tools.Logger;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
    private final Configuration config;

    public AccessResource(Configuration config) {
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
            if (expired == null) {
                return Response
                        .ok(new ErrorMessage("Missing access token"))
                        .status(400)
                        .build();
            }

            String subject = Jwts.parser()
                    .setSigningKey(Aves.getKey())
                    .setAllowedClockSkewSeconds(TimeUnit.DAYS.toSeconds(1))
                    .parseClaimsJws(expired)
                    .getBody()
                    .getSubject();

            UUID userId = UUID.fromString(subject);

            long mills = TimeUnit.SECONDS.toMillis(config.tokenExpiration);
            Date exp = new Date(new Date().getTime() + mills);

            String token = Jwts.builder()
                    .setIssuer("https://aves.services.wire.com")
                    .setSubject("" + userId)
                    .setExpiration(exp)
                    .signWith(Aves.getKey())
                    .compact();

            AccessToken result = new AccessToken();
            result.expiresIn = config.tokenExpiration;
            result.accessToken = token;
            result.tokenType = "Bearer";
            result.user = userId;

            return Response.
                    ok(result).
                    cookie(new NewCookie("zuid", result.accessToken)).
                    build();
        } catch (ExpiredJwtException e) {
            return Response
                    .ok(new ErrorMessage("Authentication failed.", 403, "invalid-credentials"))
                    .status(403)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("AccessResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage(), 500, "server-error"))
                    .status(500)
                    .build();
        }
    }

    @POST
    @Path("logout")
    @ApiOperation(value = "Calling this endpoint will effectively revoke the given cookie and subsequent calls to " +
            "/access with the same cookie will result in a 403.")
    public Response post(@CookieParam("zuid") String cookie) {
        return Response.
                ok().
                build();
    }
}
