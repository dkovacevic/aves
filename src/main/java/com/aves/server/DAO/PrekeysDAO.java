package com.aves.server.DAO;

import com.aves.server.model.otr.PreKey;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface PrekeysDAO {
    @SqlUpdate("INSERT INTO Prekeys (client_id, key_id, key) " +
            "VALUES (:clientId, :keyId, :key) " +
            "ON CONFLICT (client_Id, key_id) DO UPDATE SET key = EXCLUDED.key")
    int insert(@Bind("clientId") String clientId,
               @Bind("keyId") int keyId,
               @Bind("key") String key);

    @SqlQuery("SELECT * FROM Prekeys WHERE client_id = :clientId AND used = FALSE LIMIT 1")
    @RegisterMapper(_Mapper.class)
    PreKey get(@Bind("clientId") String clientId);

    @SqlQuery("SELECT * FROM Prekeys WHERE client_id = :clientId AND key_id = :keyId AND used = FALSE")
    @RegisterMapper(_Mapper.class)
    PreKey get(@Bind("clientId") String clientId,
               @Bind("keyId") int keyId);

    @SqlUpdate("UPDATE Prekeys SET used = TRUE WHERE client_id = :clientId AND key_id = :keyId")
    int mark(@Bind("clientId") String clientId,
             @Bind("keyId") int keyId);

    class _Mapper implements ResultSetMapper<PreKey> {
        @Override
        public PreKey map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            PreKey preKey = new PreKey();
            preKey.id = rs.getInt("key_id");
            preKey.key = rs.getString("key");

            return preKey;
        }
    }
}
