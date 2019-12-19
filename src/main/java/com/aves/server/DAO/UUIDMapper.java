package com.aves.server.DAO;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UUIDMapper implements RowMapper<UUID> {
    @Override
    public UUID map(ResultSet rs, StatementContext ctx) throws SQLException {
        UUID contact = null;
        Object rsObject = rs.getObject("uuid");
        if (rsObject != null)
            contact = (UUID) rsObject;
        return contact;
    }
}
