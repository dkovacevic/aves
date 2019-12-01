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
import org.skife.jdbi.v2.DBI;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;
import java.util.UUID;

import static com.aves.server.tools.Util.getProfilePicture;
import static com.aves.server.tools.Util.s3UploadFile;

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
    public Response post(@ApiParam @Valid NewUser newUser) {
        try {
            UserDAO userDAO = jdbi.onDemand(UserDAO.class);

            String hash = SCryptUtil.scrypt(newUser.password, 16384, 8, 1);

            UUID userId = UUID.randomUUID();

            Picture profile = getProfilePicture();
            UUID preview = s3UploadFile(profile.getImageData());

            userDAO.insert(
                    userId,
                    newUser.name,
                    newUser.firstname,
                    newUser.lastname,
                    newUser.country,
                    newUser.email.toLowerCase(),
                    newUser.phone,
                    new Random().nextInt(8),
                    preview,
                    preview,
                    hash);

            User user = userDAO.getUser(userId);

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
