package com.aves.server.DAO;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UUIDMapper implements ResultSetMapper<UUID> {
    @Override
    @Nullable
    public UUID map(int i, ResultSet rs, StatementContext statementContext) {
        try {
            return getUuid(rs, "uuid");
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
