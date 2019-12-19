package com.aves.server.resource;

import com.aves.server.Aves;
import com.aves.server.DAO.UserDAO;
import com.aves.server.model.AccessToken;
import com.aves.server.model.Configuration;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.SignIn;
import com.aves.server.tools.Logger;
import com.lambdaworks.crypto.SCryptUtil;
import io.jsonwebtoken.Jwts;
import io.swagger.annotations.*;
import org.jdbi.v3.core.Jdbi;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Api
@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {
    private final Jdbi jdbi;
    private final Configuration config;

    public LoginResource(Jdbi jdbi, Configuration config) {
        this.jdbi = jdbi;
        this.config = config;
    }

    @POST
    @ApiOperation(value = "Authenticate a user to obtain a cookie and first access token")
    @ApiResponses(value = {@ApiResponse(code = 403, message = "Wrong email or password")})
    public Response login(@ApiParam @Valid SignIn signIn) {
        try {
            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            String email = signIn.email.toLowerCase().trim();

            String hashed = userDAO.getHash(email);
            if (hashed == null || !SCryptUtil.check(signIn.password, hashed)) {
                return Response
                        .ok(new ErrorMessage("Wrong email or password"))
                        .status(403)
                        .build();
            }

            long mills = TimeUnit.SECONDS.toMillis(config.tokenExpiration);
            Date exp = new Date(new Date().getTime() + mills);

            UUID userId = userDAO.getUserId(email);
            if (userId == null) {
                return Response
                        .ok(new ErrorMessage("Wrong email or password"))
                        .status(403)
                        .build();
            }

            String token = Jwts.builder()
                    .setIssuer("https://aves.com")
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
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("LoginResource.login : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
