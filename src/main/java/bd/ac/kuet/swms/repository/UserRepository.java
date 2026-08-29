package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.Role;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.model.UserFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepository extends AbstractRepository<User> {
    public UserRepository(Database db) { super(db); }

    @Override protected String table() { return "users"; }

    @Override protected String orderBy() { return " ORDER BY t.username"; }

    @Override
    protected User map(ResultSet rs) throws SQLException {
        User u = UserFactory.create(Role.valueOf(rs.getString("role")));
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setSalt(rs.getString("salt"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setActive(rs.getInt("active") == 1);
        u.setCreatedAt(dateTime(rs, "created_at"));
        return u;
    }

    @Override
    protected PreparedStatement insert(Connection c, User u) throws SQLException {
        return prepare(c, "INSERT INTO users(username, full_name, role, salt, password_hash, active, created_at) VALUES (?,?,?,?,?,?,?)",
                u.getUsername(), u.getFullName(), u.getRole(), u.getSalt(), u.getPasswordHash(), u.isActive(), u.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, User u) throws SQLException {
        return prepare(c, "UPDATE users SET username=?, full_name=?, role=?, salt=?, password_hash=?, active=? WHERE id=?",
                u.getUsername(), u.getFullName(), u.getRole(), u.getSalt(), u.getPasswordHash(), u.isActive(), u.getId());
    }

    public Optional<User> findByUsername(String username) {
        return query(select() + " WHERE t.username = ?", username).stream().findFirst();
    }

    public boolean isEmpty() { return number("SELECT COUNT(*) FROM users") == 0; }
}
