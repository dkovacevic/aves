package com.aves.server.resource;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.NotificationsDAO;
import com.aves.server.model.ErrorMessage;
import com.aves.server.model.Event;
import com.aves.server.model.NotificationList;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.skife.jdbi.v2.DBI;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
                        @QueryParam("client") @NotNull String clientId,
                        @QueryParam("size") @DefaultValue("30") int size,
                        @QueryParam("since") UUID since) {
        try {
            UUID last = null;

            UUID userId = (UUID) context.getProperty("zuid");

            NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            //check the clientId validity
            UUID challenge = clientsDAO.getUserId(clientId);
            if (!Objects.equals(userId, challenge)) {
                return Response.
                        ok(new ErrorMessage("Unknown clientId")).
                        status(400).
                        build();
            }

            Timestamp time = since != null ? notificationsDAO.getTime(since) : new Timestamp(0);
            List<String> notifications = notificationsDAO.get(clientId, userId, time, size);

            NotificationList result = new NotificationList();
            result.notifications = new ArrayList<>();
            for (String notif : notifications) {
                Event notification = mapper.readValue(notif, Event.class);
                result.notifications.add(notification);
                last = notification.id;
            }

            clientsDAO.updateLast(clientId, last);

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

    @GET
    @Path("last")
    @ApiOperation(value = "Fetch last notification")
    @Authorization("Bearer")
    public Response last(@Context ContainerRequestContext context,
                         @QueryParam("client") @NotNull String clientId) {
        try {
            UUID userId = (UUID) context.getProperty("zuid");

            NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
            ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

            //check the clientId validity
            UUID challenge = clientsDAO.getUserId(clientId);
            if (!Objects.equals(userId, challenge)) {
                return Response.
                        ok(new ErrorMessage("Unknown clientId")).
                        status(400).
                        build();
            }

            UUID last = clientsDAO.getLast(clientId);
            if (last == null) {
                return Response
                        .ok(new Event())
                        .status(200)
                        .build();
            }

            String notification = notificationsDAO.getLast(last);
            if (notification == null) {
                return Response
                        .ok(new Event())
                        .status(200)
                        .build();
            }

            Event event = mapper.readValue(notification, Event.class);

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
