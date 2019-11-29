package com.aves.server.DAO;

import com.aves.server.model.Conversation;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public interface ConversationsDAO {
    @SqlUpdate("INSERT INTO Conversations (conv_Id, name, creator) " +
            "VALUES (:convId, :name, :creator)")
    int insert(@Bind("convId") UUID convId,
               @Bind("name") String name,
               @Bind("creator") UUID creator);

    @SqlQuery("SELECT * from Conversations WHERE conv_id = :convId")
    @RegisterMapper(_Mapper.class)
    Conversation get(@Bind("convId") UUID convId);

    class _Mapper implements ResultSetMapper<Conversation> {
        @Override
        public Conversation map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            Conversation conv = new Conversation();
            conv.id = getUuid(rs, "conv_id");
            conv.creator = getUuid(rs, "creator");
            conv.name = rs.getString("name");
            conv.type = 2;
            return conv;
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

