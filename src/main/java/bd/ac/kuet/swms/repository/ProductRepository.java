package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductRepository extends AbstractRepository<Product> {
    private static final String GROUP_BY = " GROUP BY t.id";

    public ProductRepository(Database db) { super(db); }

    @Override protected String table() { return "products"; }

    @Override
    protected String select() {
        return "SELECT t.*, c.name AS category_name FROM products t LEFT JOIN categories c ON c.id = t.category_id";
    }

    @Override
    public Optional<Product> findById(int id) {
        return query(select() + " WHERE t.id = ?" + GROUP_BY, id).stream().findFirst();
    }

    @Override
    public List<Product> findAll() {
        return query(select() + GROUP_BY + " ORDER BY t.name COLLATE NOCASE");
    }

    public Optional<Product> findBySku(String sku) {
        return query(select() + " WHERE t.sku = ?" + GROUP_BY, sku).stream().findFirst();
    }

    @Override
    protected Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategoryId(nullableInt(rs, "category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setCostPrice(rs.getDouble("cost_price"));
        p.setUnitPrice(rs.getDouble("unit_price"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        p.setQuantity(rs.getInt("quantity"));
        p.setCreatedAt(dateTime(rs, "created_at"));
        return p;
    }

    @Override
    protected PreparedStatement insert(Connection c, Product p) throws SQLException {
        return prepare(c, "INSERT INTO products(sku, name, category_id, cost_price, unit_price, quantity, reorder_level, created_at) VALUES (?,?,?,?,?,?,?,?)",
                p.getSku(), p.getName(), p.getCategoryId(), p.getCostPrice(), p.getUnitPrice(), p.getQuantity(), p.getReorderLevel(), p.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, Product p) throws SQLException {
        return prepare(c, "UPDATE products SET sku=?, name=?, category_id=?, cost_price=?, unit_price=?, quantity=?, reorder_level=? WHERE id=?",
                p.getSku(), p.getName(), p.getCategoryId(), p.getCostPrice(), p.getUnitPrice(), p.getQuantity(), p.getReorderLevel(), p.getId());
    }

}
