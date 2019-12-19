package com.aves.server.DAO;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

public interface ParticipantsDAO {
    @SqlUpdate("INSERT INTO Participants (conv_Id, user_Id) " +
            "VALUES (:convId, :userId)")
    int insert(@Bind("convId") UUID convId,
               @Bind("userId") UUID userId);

    @SqlQuery("SELECT user_Id AS uuid FROM Participants WHERE conv_Id = :convId")
    @RegisterRowMapper(UUIDMapper.class)
    List<UUID> getUsers(@Bind("convId") UUID convId);

    @SqlQuery("SELECT conv_Id AS uuid FROM Participants WHERE user_Id = :userId")
    @RegisterRowMapper(UUIDMapper.class)
    List<UUID> getConversations(@Bind("userId") UUID userId);

    @SqlQuery("SELECT conv_Id AS uuid FROM Participants WHERE user_Id = :userId AND conv_Id = :convId")
    @RegisterRowMapper(UUIDMapper.class)
    UUID isParticipant(@Bind("userId") UUID userId,
                       @Bind("convId") UUID convId);

    @SqlUpdate("DELETE FROM Participants WHERE user_Id = :userId AND conv_Id = :convId")
    void remove(@Bind("convId") UUID convId, @Bind("userId") UUID userId);
}
