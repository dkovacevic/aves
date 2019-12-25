package com.aves.server.DAO;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;

public interface PropertiesDAO {
    @SqlUpdate("INSERT INTO Properties (user_id, prop_key, prop_value) " +
            "VALUES (:userId, :key, :value) ON CONFLICT (user_id, prop_key) DO UPDATE SET prop_value = EXCLUDED.prop_value")
    int upsert(@Bind("userId") UUID userId,
               @Bind("key") String key,
               @Bind("value") String value);

    @SqlQuery("SELECT prop_value FROM Properties WHERE prop_key = :key AND user_id = :userId")
    String get(@Bind("userId") UUID userId,
               @Bind("key") String key);
}
