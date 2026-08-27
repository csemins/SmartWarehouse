package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoryRepository extends AbstractRepository<Category> {
    public CategoryRepository(Database db) { super(db); }

    @Override protected String table() { return "categories"; }

    @Override protected String orderBy() { return " ORDER BY t.name"; }

    @Override
    protected Category map(ResultSet rs) throws SQLException {
        Category c = new Category(rs.getString("name"));
        c.setId(rs.getInt("id"));
        c.setCreatedAt(dateTime(rs, "created_at"));
        return c;
    }

    @Override
    protected PreparedStatement insert(Connection c, Category e) throws SQLException {
        return prepare(c, "INSERT INTO categories(name, created_at) VALUES (?,?)", e.getName(), e.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, Category e) throws SQLException {
        return prepare(c, "UPDATE categories SET name=? WHERE id=?", e.getName(), e.getId());
    }

    public Category findOrCreate(String name) {
        return query(select() + " WHERE t.name = ?", name.trim()).stream().findFirst()
                .orElseGet(() -> save(new Category(name.trim())));
    }
}
