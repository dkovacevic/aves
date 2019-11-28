package com.aves.server.DAO;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

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
            "WHERE user_id = :userId AND client_id = :clientId AND time > :time " +
            "ORDER by Time LIMIT :size")
    List<String> get(@Bind("clientId") String clientId,
                     @Bind("userId") UUID userId,
                     @Bind("time") Timestamp since,
                     @Bind("size") int size);

    @SqlQuery("SELECT time from Notifications WHERE id = :id")
    @RegisterMapper(_Mapper.class)
    Timestamp getTime(@Bind("id") UUID id);

    class _Mapper implements ResultSetMapper<Timestamp> {
        @Override
        public Timestamp map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            return rs.getTimestamp("time");
        }
    }
}

