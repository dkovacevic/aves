package com.aves.server.DAO;

import com.aves.server.model.Device;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ClientsDAO {
    @SqlUpdate("INSERT INTO Clients (client_Id, user_id, lastkey) " +
            "VALUES (:clientId, :userId, :lastKeyId)")
    int insert(@Bind("clientId") String clientId,
               @Bind("userId") UUID userId,
               @Bind("lastKeyId") int lastKeyId);

    @SqlQuery("SELECT client_Id FROM Clients WHERE user_id = :userId")
    List<String> getClients(@Bind("userId") UUID userId);

    @SqlQuery("SELECT * FROM Clients WHERE user_id = :userId")
    @RegisterMapper(_Mapper.class)
    List<Device> getDevices(@Bind("userId") UUID userId);

    @SqlQuery("SELECT * FROM Clients WHERE user_id = :userId AND client_id = :clientId")
    @RegisterMapper(_Mapper.class)
    Device getDevice(@Bind("userId") UUID userId, @Bind("clientId") String clientId);

    @SqlQuery("SELECT user_id AS uuid FROM Clients WHERE client_id = :clientId")
    @RegisterMapper(UUIDMapper.class)
    UUID getUserId(@Bind("clientId") String clientId);

    @SqlQuery("SELECT last AS uuid FROM Clients WHERE client_id = :clientId")
    @RegisterMapper(UUIDMapper.class)
    UUID getLast(@Bind("clientId") String clientId);

    @SqlUpdate("UPDATE Clients SET last = :last WHERE client_id = :clientId")
    int updateLast(@Bind("clientId") String clientId,
                   @Bind("last") UUID last);

    class _Mapper implements ResultSetMapper<Device> {
        @Override
        @Nullable
        public Device map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            Device device = new Device();
            device.id = rs.getString("client_Id");
            device.time = rs.getString("time");
            device.clazz = "desktop";
            device.type = "permanent";
            device.label = "diggy";
            device.lastKey = rs.getInt("lastkey");
            return device;
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
