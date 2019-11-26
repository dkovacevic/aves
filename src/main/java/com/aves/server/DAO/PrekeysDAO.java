package com.aves.server.DAO;

import com.aves.server.model.otr.PreKey;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public interface PrekeysDAO {
    @SqlUpdate("INSERT INTO Prekeys (client_id, key_id, key) " +
            "VALUES (:clientId, :keyId, :key)")
    int insert(@Bind("clientId") String clientId,
               @Bind("keyId") int keyId,
               @Bind("key") String key);

    @SqlQuery("Select * from Prekeys WHERE client_id = :clientId")
    @RegisterMapper(_Mapper.class)
    PreKey get(@Bind("clientId") String clientId);

    class _Mapper implements ResultSetMapper<PreKey> {
        @Override
        @Nullable
        public PreKey map(int i, ResultSet rs, StatementContext statementContext) {
            try {
                PreKey preKey = new PreKey();
                preKey.id = rs.getInt("key_id");
                preKey.key = rs.getString("key");

                return preKey;
            } catch (SQLException e) {
                return null;
            }
        }

        private UUID getUuid(ResultSet rs, String name) throws SQLException {
            UUID contact = null;
            Object rsObject = rs.getObject(name);
            if (rsObject != null)
                contact = (UUID) rsObject;
            return contact;
        }
    }
}
