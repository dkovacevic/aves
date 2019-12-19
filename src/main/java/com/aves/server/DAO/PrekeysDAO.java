package com.aves.server.DAO;

import com.aves.server.model.otr.PreKey;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface PrekeysDAO {
    @SqlUpdate("INSERT INTO Prekeys (client_id, key_id, key) " +
            "VALUES (:clientId, :preKey.id, :preKey.key) " +
            "ON CONFLICT (client_Id, preKey.id) DO UPDATE SET key = EXCLUDED.key")
    int insert(@Bind("clientId") String clientId,
               @BindFields("preKey") PreKey preKey);

    @SqlQuery("SELECT * FROM Prekeys WHERE client_id = :clientId AND used = FALSE LIMIT 1")
    @RegisterRowMapper(_Mapper.class)
    PreKey get(@Bind("clientId") String clientId);

    @SqlQuery("SELECT * FROM Prekeys WHERE client_id = :clientId AND key_id = :keyId AND used = FALSE")
    @RegisterRowMapper(_Mapper.class)
    PreKey get(@Bind("clientId") String clientId,
               @Bind("keyId") int keyId);

    @SqlUpdate("UPDATE Prekeys SET used = TRUE WHERE client_id = :clientId AND key_id = :keyId")
    int mark(@Bind("clientId") String clientId,
             @Bind("keyId") int keyId);

    class _Mapper implements RowMapper<PreKey> {
        @Override
        public PreKey map(ResultSet rs, StatementContext ctx) throws SQLException {
            PreKey preKey = new PreKey();
            preKey.id = rs.getInt("key_id");
            preKey.key = rs.getString("key");
            return preKey;
        }
    }
}
