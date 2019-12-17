package com.aves.server.resource;

import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

@Api
@Path("/search/contacts")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    public SearchResource(DBI jdbi) {
    }

    @GET
    @ApiOperation(value = "Search users")
    @Authorization("Bearer")
    public Response search(@Context ContainerRequestContext context) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");
            return Response.
                    ok(new SearchResult()).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("SearchResource.search : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    static class SearchResult {
        public int took;
        public int found;
        public int returned;
        public ArrayList<User> documents = new ArrayList<>();
    }
}
