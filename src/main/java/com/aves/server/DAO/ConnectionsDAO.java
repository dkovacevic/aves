package com.aves.server.DAO;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

public interface ConnectionsDAO {
    @SqlUpdate("INSERT INTO Connections (user_from, user_to) " +
            "VALUES (:from, :to)")
    int insert(@Bind("from") UUID from,
               @Bind("to") UUID to);

    @SqlQuery("SELECT user_to AS uuid FROM Connections WHERE user_from = :from")
    @RegisterRowMapper(UUIDMapper.class)
    List<UUID> getConnections(@Bind("from") UUID from);
}
