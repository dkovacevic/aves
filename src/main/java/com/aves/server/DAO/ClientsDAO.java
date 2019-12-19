package com.aves.server.DAO;

import com.aves.server.model.Device;
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

public interface ClientsDAO {
    @SqlUpdate("INSERT INTO Clients (client_Id, user_id, lastkey) " +
            "VALUES (:clientId, :userId, :lastKeyId) " +
            "ON CONFLICT (client_Id) DO UPDATE SET lastkey = EXCLUDED.lastkey")
    int insert(@Bind("clientId") String clientId,
               @Bind("userId") UUID userId,
               @Bind("lastKeyId") int lastKeyId);

    @SqlQuery("SELECT client_Id FROM Clients WHERE user_id = :userId")
    List<String> getClients(@Bind("userId") UUID userId);

    @SqlQuery("SELECT * FROM Clients WHERE user_id = :userId")
    @RegisterRowMapper(_Mapper.class)
    List<Device> getDevices(@Bind("userId") UUID userId);

    @SqlQuery("SELECT * FROM Clients WHERE user_id = :userId AND client_id = :clientId")
    @RegisterRowMapper(_Mapper.class)
    Device getDevice(@Bind("userId") UUID userId, @Bind("clientId") String clientId);

    @SqlQuery("SELECT user_id AS uuid FROM Clients WHERE client_id = :clientId")
    @RegisterRowMapper(UUIDMapper.class)
    UUID getUserId(@Bind("clientId") String clientId);

    class _Mapper implements RowMapper<Device> {
        @Override
        public Device map(ResultSet rs, StatementContext ctx) throws SQLException {
            Device device = new Device();
            device.id = rs.getString("client_Id");
            device.time = rs.getString("time");
            device.clazz = "desktop";
            device.type = "permanent";
            device.label = "diggy";
            device.lastKey = rs.getInt("lastkey");
            return device;
        }
    }
}
