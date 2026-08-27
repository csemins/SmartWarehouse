package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.DataAccessException;
import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.BaseEntity;
import bd.ac.kuet.swms.model.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractRepository<T extends BaseEntity> implements Repository<T> {
    protected final Database db;

    protected AbstractRepository(Database db) { this.db = db; }

    // subclasses only give sql + mapping, crud lives here
    protected abstract String table();

    protected abstract T map(ResultSet rs) throws SQLException;

    protected abstract PreparedStatement insert(Connection c, T entity) throws SQLException;

    protected abstract PreparedStatement update(Connection c, T entity) throws SQLException;

    protected String select() { return "SELECT t.* FROM " + table() + " t"; }

    protected String orderBy() { return " ORDER BY t.id"; }

    @Override
    public T save(T entity) {
        try (Connection c = db.connect()) {
            return save(c, entity);
        } catch (SQLException e) {
            throw new DataAccessException(friendlyMessage(e), e);
        }
    }

    public T save(Connection c, T entity) throws SQLException {
        try {
            entity.validate();
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        try (PreparedStatement ps = entity.isNew() ? insert(c, entity) : update(c, entity)) {
            ps.executeUpdate();
            if (entity.isNew()) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) entity.setId(keys.getInt(1));
                }
            }
        }
        return entity;
    }

    @Override
    public Optional<T> findById(int id) {
        return query(select() + " WHERE t.id = ?", id).stream().findFirst();
    }

    @Override
    public List<T> findAll() {
        return query(select() + orderBy());
    }

    @Override
    public boolean delete(int id) {
        return execute("DELETE FROM " + table() + " WHERE id = ?", id) > 0;
    }

    protected List<T> query(String sql, Object... params) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException(friendlyMessage(e), e);
        }
    }

    protected int execute(String sql, Object... params) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(friendlyMessage(e), e);
        }
    }

    protected double number(String sql, Object... params) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException(friendlyMessage(e), e);
        }
    }

    protected static PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        bind(ps, params);
        return ps;
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            // dates stored as ISO text, sqlite has no real date type
            if (p instanceof Enum<?> e) ps.setString(i + 1, e.name());
            else if (p instanceof LocalDateTime || p instanceof LocalDate) ps.setString(i + 1, p.toString());
            else if (p instanceof Boolean b) ps.setInt(i + 1, b ? 1 : 0);
            else ps.setObject(i + 1, p);
        }
    }

    protected static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : LocalDateTime.parse(value);
    }

    protected static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    // raw sqlite errors are unreadable for users
    private static String friendlyMessage(SQLException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("UNIQUE")) return "A record with the same value already exists.";
        if (m.contains("FOREIGN KEY")) return "This record is still used (for example a product that is in the rack or in an order).";
        if (m.contains("CHECK")) return "A value is not allowed.";
        return m;
    }
}
