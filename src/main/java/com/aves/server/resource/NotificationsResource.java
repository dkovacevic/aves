package com.aves.server.resource;

import com.aves.server.DAO.NotificationsDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.NotificationList;
import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.jdbi.v3.core.Jdbi;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.UUID;

@Api
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationsResource {
    private final Jdbi jdbi;
    private ObjectMapper mapper = new ObjectMapper();
    private final NotificationsDAO notificationsDAO;

    public NotificationsResource(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
    }

    @GET
    @ApiOperation(value = "Fetch notifications")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context,
                        @QueryParam("client") @NotNull String clientId,
                        @QueryParam("size") @DefaultValue("30") int size,
                        @QueryParam("since") UUID since) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            NotificationList result = new NotificationList();
            result.time = Util.time();

            int status = 200;
            Timestamp time = null;
            if (since != null) {
                time = notificationsDAO.getTime(since);
                if (time == null) {
                    status = 404;
                }
            }

            if (time == null) {
                time = new Timestamp(0);
            }

            result.notifications = notificationsDAO.get(clientId, userId, time, size);
            Logger.debug("NotificationsResource::get: %s %s size: %d", userId, clientId, result.notifications.size());

            return Response.
                    ok(result).
                    status(status).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("NotificationsResource.get : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }

    @GET
    @Path("last")
    @ApiOperation(value = "Fetch last notification")
    @Authorization("Bearer")
    public Response last(@Context ContainerRequestContext context,
                         @QueryParam("client") @NotNull String clientId) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);

            Event event = notificationsDAO.getLast(clientId, userId);

            return Response.
                    ok(event).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("NotificationsResource.last : %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
