package com.aves.server.DAO;

import com.aves.server.model.User;
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

public interface UserDAO {
    @SqlUpdate("INSERT INTO Users (user_id, name, firstname, lastname, country, email, phone, accent, complete, preview, hash) " +
            "VALUES (:userId, :name, :firstname, :lastname, :country, :email, :phone, :accent, :complete, :preview, :hash)")
    int insert(@Bind("userId") UUID userId,
               @Bind("name") String name,
               @Bind("firstname") String firstname,
               @Bind("lastname") String lastname,
               @Bind("country") String country,
               @Bind("email") String email,
               @Bind("phone") String phone,
               @Bind("accent") int accent,
               @Bind("complete") UUID complete,
               @Bind("preview") UUID preview,
               @Bind("hash") String hash);

    @SqlQuery("SELECT hash FROM Users WHERE email = :email")
    String getHash(@Bind("email") String email);

    @SqlQuery("SELECT user_id AS uuid FROM Users WHERE email = :email")
    @RegisterMapper(UUIDMapper.class)
    UUID getUserId(@Bind("email") String email);

    @SqlQuery("SELECT * FROM Users WHERE user_id = :userId")
    @RegisterMapper(_Mapper.class)
    User getUser(@Bind("userId") UUID userId);

    class _Mapper implements ResultSetMapper<User> {
        @Override
        @Nullable
        public User map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            User user = new User();
            user.id = getUuid(rs, "user_id");
            user.name = rs.getString("name");
            user.firstname = rs.getString("firstname");
            user.lastname = rs.getString("lastname");
            user.country = rs.getString("country");
            user.phone = rs.getString("phone");
            user.email = rs.getString("email");
            user.accent = rs.getInt("accent");

            UUID complete = getUuid(rs, "complete");
            if (complete != null) {
                User.UserAsset asset = new User.UserAsset();
                asset.size = "complete";
                asset.key = complete;
                user.assets.add(asset);
            }

            UUID preview = getUuid(rs, "preview");
            if (preview != null) {
                User.UserAsset asset = new User.UserAsset();
                asset.size = "preview";
                asset.key = preview;
                user.assets.add(asset);
            }
            return user;
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
