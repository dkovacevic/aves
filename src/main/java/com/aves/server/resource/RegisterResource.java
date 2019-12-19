package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.NewUser;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Picture;
import com.lambdaworks.crypto.SCryptUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jdbi.v3.core.Jdbi;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static com.aves.server.tools.Util.*;

@Api
@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
public class RegisterResource {
    private final Jdbi jdbi;

    public RegisterResource(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @POST
    @ApiOperation(value = "Register new user")
    public Response post(@ApiParam @Valid NewUser newUser) {
        try {
            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            String hash = SCryptUtil.scrypt(newUser.password, 16384, 8, 1);

            Picture profile = getProfilePicture();
            String preview = s3UploadFile(profile.getImageData());

            User user = new User();
            user.id = UUID.randomUUID();
            user.name = newUser.name;
            user.firstname = newUser.firstname;
            user.lastname = newUser.lastname;
            user.email = newUser.email.toLowerCase().trim();
            user.country = newUser.country;
            user.phone = newUser.phone;
            user.accent = random(8);

            userDAO.insert(
                    user,
                    preview,
                    preview,
                    hash);

            userDAO.setResetPassword(user.id, false);
            
            return Response.
                    ok(user).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("RegisterResource.post : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
