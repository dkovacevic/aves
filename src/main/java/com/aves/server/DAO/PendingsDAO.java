package com.aves.server.DAO;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface PendingsDAO {
    @SqlUpdate("INSERT INTO Pendings(response_id, user_id) VALUES (:id, :user)")
    int insert(@Bind("id") UUID id,
               @Bind("user") UUID user);

    @SqlUpdate("DELETE FROM Pendings WHERE response_id = :id")
    int delete(@Bind("id") UUID id);

    @SqlQuery("SELECT response_id AS uuid FROM Pendings")
    @RegisterRowMapper(_Mapper.class)
    List<UUID> getRequests();

    @SqlQuery("SELECT user_id AS uuid FROM Pendings WHERE response_id = :id")
    @RegisterRowMapper(_Mapper.class)
    UUID getUserId(@Bind("id") UUID id);

    class _Mapper implements RowMapper<UUID> {
        @Override
        public UUID map(ResultSet rs, StatementContext ctx) throws SQLException {
            return (UUID) rs.getObject("uuid");
        }
    }
}

