package com.aves.server.resource;

import com.aves.server.DAO.UserDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Api
@Path("/search/contacts")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {
    private final UserDAO userDAO;

    public SearchResource(Jdbi jdbi) {
        userDAO = jdbi.onDemand(UserDAO.class);
    }

    @GET
    @ApiOperation(value = "Search users")
    @Authorization("Bearer")
    public Response search(@Context ContainerRequestContext context,
                           @QueryParam("q") String keyword) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");
            SearchResult result = new SearchResult();
            result.documents = userDAO.search(keyword);
            result.found = result.documents.size();
            result.returned = result.documents.size();
            return Response.
                    ok(result).
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
        public List<User> documents;
    }
}
