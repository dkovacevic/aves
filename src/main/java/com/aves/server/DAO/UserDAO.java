package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.UUID;

public interface UserDAO {
    @SqlUpdate("INSERT INTO Users (user_id, name, email, hash) " +
            "VALUES (:userId, :name, :email, :hash)")
    int insert(@Bind("userId") UUID userId,
               @Bind("name") String name,
               @Bind("email") String email,
               @Bind("hash") String hash);

    @SqlQuery("SELECT hash FROM Users WHERE email = :email")
    String getHash(@Bind("email") String email);

    @SqlQuery("SELECT user_id AS uuid FROM Users WHERE email = :email")
    @RegisterMapper(UUIDMapper.class)
    UUID getUserId(@Bind("email") String email);
}
