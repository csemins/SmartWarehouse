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
    // quantity is not stored, it's the sum of the occupied rack cells
    protected String select() {
        return "SELECT t.*, c.name AS category_name, COALESCE(SUM(r.quantity), 0) AS quantity, COUNT(r.id) AS boxes "
                + "FROM products t LEFT JOIN categories c ON c.id = t.category_id "
                + "LEFT JOIN rack_cells r ON r.product_id = t.id AND r.status = 'OCCUPIED'";
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
        p.setUnitsPerBox(rs.getInt("units_per_box"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        p.setImageUrl(rs.getString("image_url"));
        p.setQuantity(rs.getInt("quantity"));
        p.setBoxes(rs.getInt("boxes"));
        p.setCreatedAt(dateTime(rs, "created_at"));
        return p;
    }

    @Override
    protected PreparedStatement insert(Connection c, Product p) throws SQLException {
        return prepare(c, "INSERT INTO products(sku, name, category_id, cost_price, unit_price, units_per_box, reorder_level, image_url, created_at) VALUES (?,?,?,?,?,?,?,?,?)",
                p.getSku(), p.getName(), p.getCategoryId(), p.getCostPrice(), p.getUnitPrice(), p.getUnitsPerBox(), p.getReorderLevel(), p.getImageUrl(), p.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, Product p) throws SQLException {
        return prepare(c, "UPDATE products SET sku=?, name=?, category_id=?, cost_price=?, unit_price=?, units_per_box=?, reorder_level=?, image_url=? WHERE id=?",
                p.getSku(), p.getName(), p.getCategoryId(), p.getCostPrice(), p.getUnitPrice(), p.getUnitsPerBox(), p.getReorderLevel(), p.getImageUrl(), p.getId());
    }

}
