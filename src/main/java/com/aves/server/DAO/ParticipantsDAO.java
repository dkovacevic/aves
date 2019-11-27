package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;
import java.util.UUID;

public interface ParticipantsDAO {
    @SqlUpdate("INSERT INTO Participants (conv_Id, user_Id) " +
            "VALUES (:convId, :userId)")
    int insert(@Bind("convId") UUID convId,
               @Bind("userId") UUID userId);

    @SqlQuery("SELECT user_Id AS uuid FROM Participants WHERE conv_Id = :convId ")
    @RegisterMapper(UUIDMapper.class)
    List<UUID> getUsers(@Bind("convId") UUID convId);

    @SqlQuery("SELECT conv_Id AS uuid FROM Participants WHERE user_Id = :userId ")
    @RegisterMapper(UUIDMapper.class)
    List<UUID> getConversations(@Bind("userId") UUID userId);
}
