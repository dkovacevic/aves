package com.aves.server.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface PrekeysDAO {
    @SqlUpdate("INSERT INTO Prekeys (client_id, key_id, key) " +
            "VALUES (:clientId, :keyId, :key)")
    int insert(@Bind("client_id") String clientId,
               @Bind("keyId") int id,
               @Bind("key") String key);

}
