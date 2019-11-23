package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.UUID;

public interface ConversationsDAO {
    @SqlUpdate("INSERT INTO Conversations (id, name, creator) " +
            "VALUES (:convId, :name, :creator)")
    int insert(@Bind("convId") UUID convId,
               @Bind("name") String name,
               @Bind("creator") UUID creator);

}
