package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;
import java.util.UUID;

public interface ClientsDAO {
    @SqlUpdate("INSERT INTO Clients (client_Id, user_id, lastkey) " +
            "VALUES (:clientId, :userId, :lastKeyId)")
    int insert(@Bind("clientId") String clientId,
               @Bind("userId") UUID userId,
               @Bind("lastKeyId") int lastKeyId);

    @SqlQuery("SELECT client_Id FROM Clients WHERE user_id = :userId")
    List<String> getClients(@Bind("userId") UUID userId);

    @SqlQuery("SELECT user_id AS uuid FROM Clients WHERE client_id = :clientId")
    @RegisterMapper(UUIDMapper.class)
    UUID getUserId(@Bind("clientId") String clientId);
}
