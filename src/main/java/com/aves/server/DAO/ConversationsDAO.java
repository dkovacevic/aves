package com.aves.server.DAO;

import com.aves.server.model.Conversation;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public interface ConversationsDAO {
    @SqlUpdate("INSERT INTO Conversations (conv_Id, name, creator, type) " +
            "VALUES (:conv.id, :conv.name, :conv.creator, :conv.type)")
    int insert(@BindFields("conv") Conversation conv);

    @SqlQuery("SELECT * from Conversations WHERE conv_id = :convId")
    @RegisterRowMapper(_Mapper.class)
    Conversation get(@Bind("convId") UUID convId);

    class _Mapper implements RowMapper<Conversation> {
        @Override
        public Conversation map(ResultSet rs, StatementContext ctx) throws SQLException {
            Conversation conv = new Conversation();
            conv.id = (UUID) rs.getObject("conv_id");
            conv.creator = (UUID) rs.getObject("creator");
            conv.name = rs.getString("name");
            conv.type = rs.getInt("type");
            return conv;
        }
    }
}

