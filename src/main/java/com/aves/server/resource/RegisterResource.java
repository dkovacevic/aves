package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.Logger;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.NewUser;
import com.lambdaworks.crypto.SCryptUtil;
import io.swagger.annotations.*;
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
      import java.util.UUID;
@Api
@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
public class RegisterResource {
    private final DBI jdbi;

    public RegisterResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Register new user")
    @ApiResponses(value = {@ApiResponse(code = 403, message = "Wrong email or password")})
    public Response register(@ApiParam @Valid NewUser newUser) {
        try {
            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            String hash = SCryptUtil.scrypt(newUser.password, 16384, 8, 1);

            UUID userId = UUID.randomUUID();

            int insert = userDAO.insert(userId, newUser.name, newUser.email, hash);

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("RegisterResource.register : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
