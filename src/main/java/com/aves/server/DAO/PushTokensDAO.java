package com.aves.server.DAO;

import com.aves.server.model.PushToken;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface PushTokensDAO {
    @SqlUpdate("INSERT INTO PushTokens(token, user_id, client, app, transport) " +
            "VALUES (:token, :user, :client, :app, :transport)")
    int insert(@Bind("token") String token,
               @Bind("user") UUID user,
               @Bind("client") String client,
               @Bind("app") String app,
               @Bind("transport") String transport
    );

    @SqlUpdate("DELETE FROM PushTokens WHERE token = :token AND user_id = :user")
    int delete(@Bind("token") String token,
               @Bind("user") UUID user);

    @SqlQuery("SELECT * FROM PushTokens WHERE user_id = :user")
    @RegisterRowMapper(_Mapper.class)
    List<PushToken> getPushTokens(@Bind("user") UUID user);

    class _Mapper implements RowMapper<PushToken> {
        @Override
        public PushToken map(ResultSet rs, StatementContext ctx) throws SQLException {
            PushToken ret = new PushToken();
            ret.token = rs.getString("token");
            ret.client = rs.getString("client");
            ret.app = rs.getString("app");
            ret.transport = rs.getString("transport");
            return ret;
        }
    }
}

