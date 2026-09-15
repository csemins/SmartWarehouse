package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.AuditEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AuditRepository extends AbstractRepository<AuditEntry> {
    public AuditRepository(Database db) { super(db); }

    @Override protected String table() { return "audit_log"; }

    @Override protected String orderBy() { return " ORDER BY t.id DESC"; }

    @Override
    protected AuditEntry map(ResultSet rs) throws SQLException {
        AuditEntry e = new AuditEntry(nullableInt(rs, "user_id"), rs.getString("username"),
                AuditEntry.Category.valueOf(rs.getString("category")), rs.getString("action"));
        e.setId(rs.getInt("id"));
        e.setCreatedAt(dateTime(rs, "created_at"));
        return e;
    }

    @Override
    protected PreparedStatement insert(Connection c, AuditEntry e) throws SQLException {
        return prepare(c, "INSERT INTO audit_log(user_id, username, category, action, created_at) VALUES (?,?,?,?,?)",
                e.getUserId(), e.getUsername(), e.getCategory(), e.getAction(), e.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, AuditEntry e) throws SQLException {
        throw new SQLException("Audit entries cannot be changed");
    }

    public List<AuditEntry> latest(int limit) { return query(select() + orderBy() + " LIMIT ?", limit); }
}
