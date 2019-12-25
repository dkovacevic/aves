package com.aves.server.DAO;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface PropertiesDAO {
    @SqlUpdate("INSERT INTO Properties (prop_key, prop_value) " +
            "VALUES (:key, :value) ON CONFLICT (prop_key) DO UPDATE SET prop_value = EXCLUDED.prop_value")
    int upsert(@Bind("key") String key,
               @Bind("value") String value);

    @SqlQuery("SELECT prop_value FROM Properties WHERE prop_key = :key")
    String get(@Bind("key") String key);
}
