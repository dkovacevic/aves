package com.aves.server.DAO;

import com.aves.server.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public interface NotificationsDAO {
    @SqlUpdate("INSERT INTO Notifications (id, client_Id, user_id, notification) " +
            "VALUES (:id, :clientId, :userId, :notification)")
    int insert(@Bind("id") UUID id,
               @Bind("clientId") String clientId,
               @Bind("userId") UUID userId,
               @Bind("notification") String notification);

    @SqlQuery("SELECT notification from Notifications " +
            "WHERE user_id = :userId " +
            "AND coalesce(client_id, :clientId) = :clientId " +
            "AND time > :since " +
            "ORDER by Time LIMIT :size")
    @RegisterRowMapper(_Mapper.class)
    List<Event> get(@Bind("clientId") String clientId,
                    @Bind("userId") UUID userId,
                    @Bind("since") Timestamp since,
                    @Bind("size") int size);

    @SqlQuery("SELECT notification FROM Notifications " +
            "WHERE user_id = :userId " +
            "AND coalesce(client_id, :clientId) = :clientId " +
            "AND time = ( SELECT MIN(N.time) " +
            "             FROM Notifications N " +
            "             WHERE N.user_id = :userId " +
            "             AND coalesce(N.client_id, :clientId) = :clientId )")
    @RegisterRowMapper(_Mapper.class)
    Event getLast(@Bind("clientId") String clientId,
                  @Bind("userId") UUID userId);

    @SqlQuery("SELECT time FROM Notifications WHERE id = :id")
    Timestamp getTime(@Bind("id") UUID id);

    class _Mapper implements RowMapper<Event> {
        private static ObjectMapper mapper = new ObjectMapper();

        @Override
        public Event map(ResultSet rs, StatementContext ctx) throws SQLException {
            try {
                return mapper.readValue(rs.getString("notification"), Event.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}

