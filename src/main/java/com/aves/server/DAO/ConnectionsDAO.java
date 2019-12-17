package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;
import java.util.UUID;

public interface ConnectionsDAO {
    @SqlUpdate("INSERT INTO Connections (user_from, user_to) " +
            "VALUES (:from, :to)")
    int insert(@Bind("from") UUID from,
               @Bind("to") UUID to);

    @SqlQuery("SELECT user_to AS uuid FROM Connections WHERE user_from = :from")
    @RegisterMapper(UUIDMapper.class)
    List<UUID> getConnections(@Bind("from") UUID from);
}
