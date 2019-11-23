package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface PrekeysDAO {
    @SqlUpdate("INSERT INTO Prekeys (id, key) " +
            "VALUES (:id, :key)")
    int insert(@Bind("id") int id,
               @Bind("key") String key);

}
