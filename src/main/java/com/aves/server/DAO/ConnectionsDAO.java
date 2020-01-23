package com.aves.server.DAO;

import com.aves.server.model.Connection;
import com.aves.server.tools.Util;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ConnectionsDAO {
    @SqlUpdate("INSERT INTO Connections (user_from, user_to, conv) " +
            "VALUES (:from, :to, :conv)")
    int insert(@Bind("from") UUID from,
               @Bind("to") UUID to,
               @Bind("conv") UUID conv);

    @SqlQuery("SELECT *  FROM Connections WHERE user_from = :from")
    @RegisterRowMapper(_Mapper.class)
    List<Connection> getConnections(@Bind("from") UUID from);

    class _Mapper implements RowMapper<Connection> {
        @Override
        public Connection map(ResultSet rs, StatementContext ctx) throws SQLException {
            Connection connection = new Connection();
            Date time = rs.getDate("time");
            connection.time = Util.time(time);
            connection.from = (UUID) rs.getObject("user_from");
            connection.to = (UUID) rs.getObject("user_to");
            Object conv = rs.getObject("conv");
            if (conv != null)
                connection.conversation = (UUID) conv;

            return connection;
        }
    }
}
