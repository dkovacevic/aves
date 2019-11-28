package com.aves.server.resource;

import com.aves.server.DAO.NotificationsDAO;
import com.aves.server.Logger;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.NotificationList;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Api
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationsResource {
    private final DBI jdbi;
    private ObjectMapper mapper = new ObjectMapper();

    public NotificationsResource(DBI jdbi) {
        this.jdbi = jdbi;
    }

    @GET
    @ApiOperation(value = "Fetch notifications")
    @Authorization("Bearer")
    public Response get(@Context ContainerRequestContext context,
                        @QueryParam("client") String clientId,
                        @QueryParam("size") int size,
                        @QueryParam("last") UUID last) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);

            Timestamp time = notificationsDAO.getTime(last);
            List<String> notifications = notificationsDAO.get(clientId, userId, time, size);

            NotificationList result = new NotificationList();
            result.notifications = new ArrayList<>();
            for (String notif : notifications) {
                Event notification = mapper.readValue(notif, Event.class);
                result.notifications.add(notification);
            }
            return Response.
                    ok(result).
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
}
