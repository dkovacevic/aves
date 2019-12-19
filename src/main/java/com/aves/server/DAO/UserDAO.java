package com.aves.server.DAO;

import com.aves.server.model.User;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface UserDAO {
    @SqlUpdate("INSERT INTO Users (user_id, name, firstname, lastname, country, email, phone, accent, complete, preview, hash) " +
            "VALUES (:user.id, :user.name, :user.firstname, :user.lastname, :user.country, :user.email, :user.phone, :user.accent," +
            " :complete, :preview, :hash)")
    int insert(@BindFields("user") User user,
               @Bind("complete") String complete,
               @Bind("preview") String preview,
               @Bind("hash") String hash);

    @SqlQuery("SELECT hash FROM Users WHERE email = :email")
    String getHash(@Bind("email") String email);

    @SqlQuery("SELECT hash FROM Users WHERE user_id = :userId")
    String getHash(@Bind("userId") UUID userId);

    @SqlQuery("SELECT user_id AS uuid FROM Users WHERE email = :email")
    @RegisterRowMapper(UUIDMapper.class)
    UUID getUserId(@Bind("email") String email);

    @SqlQuery("SELECT * FROM Users WHERE user_id = :userId")
    @RegisterRowMapper(_Mapper.class)
    User getUser(@Bind("userId") UUID userId);

    @SqlQuery("SELECT password_reset FROM Users WHERE user_id = :userId")
    Boolean getResetPassword(@Bind("userId") UUID userId);

    @SqlUpdate("UPDATE Users SET password_reset = :reset WHERE user_id = :userId")
    int setResetPassword(@Bind("userId") UUID userId, @Bind("reset") boolean reset);

    @SqlUpdate("UPDATE Users SET hash = :hash, password_reset = false WHERE user_id = :userId")
    int updateHash(@Bind("userId") UUID userId,
                   @Bind("hash") String hash);

    @SqlQuery("SELECT * FROM Users WHERE name ~* :keyword")
    @RegisterRowMapper(_Mapper.class)
    List<User> search(@Bind("keyword") String keyword);

    class _Mapper implements RowMapper<User> {
        @Override
        public User map(ResultSet rs, StatementContext ctx) throws SQLException {
            User user = new User();
            user.id = (UUID) rs.getObject("user_id");
            user.name = rs.getString("name");
            user.firstname = rs.getString("firstname");
            user.lastname = rs.getString("lastname");
            user.country = rs.getString("country");
            user.phone = rs.getString("phone");
            user.email = rs.getString("email");
            user.accent = rs.getInt("accent");

            String complete = rs.getString("complete");
            if (complete != null) {
                User.UserAsset asset = new User.UserAsset();
                asset.size = "complete";
                asset.key = complete;
                user.assets.add(asset);
            }

            String preview = rs.getString("preview");
            if (preview != null) {
                User.UserAsset asset = new User.UserAsset();
                asset.size = "preview";
                asset.key = preview;
                user.assets.add(asset);
            }
            return user;
        }
    }
}
