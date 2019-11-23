package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.UUID;

public interface UserDAO {
    @SqlUpdate("INSERT INTO Users (id, name, email, hash) " +
            "VALUES (:id, :name, :email, :hash)")
    int insert(@Bind("id") UUID id,
               @Bind("name") String name,
               @Bind("email") String email,
               @Bind("hash") String hash);

    @SqlQuery("SELECT hash FROM Users WHERE email = :email")
    String getHash(@Bind("email") String email);

    @SqlQuery("SELECT id FROM Users WHERE email = :email")
    UUID getUserId(@Bind("email") String email);
}
