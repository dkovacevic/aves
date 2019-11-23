package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.List;
import java.util.UUID;

public interface ParticipantsDAO {
    @SqlUpdate("INSERT INTO Participants (conv_Id, user_Id) " +
            "VALUES (:convId, :userId)")
    int insert(@Bind("convId") UUID convId,
               @Bind("userId") UUID userId);

    @SqlQuery("SELECT user_Id FROM Participants WHERE conv_Id = :convId ")
    List<UUID> get(@Bind("convId") UUID convId);
}
