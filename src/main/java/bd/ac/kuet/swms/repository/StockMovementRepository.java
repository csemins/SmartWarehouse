package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StockMovementRepository extends AbstractRepository<StockMovement> {
    public StockMovementRepository(Database db) { super(db); }

    @Override protected String table() { return "stock_movements"; }

    @Override
    protected String select() {
        return "SELECT t.*, p.name AS product_name FROM stock_movements t JOIN products p ON p.id = t.product_id";
    }

    @Override protected String orderBy() { return " ORDER BY t.id DESC"; }

    @Override
    protected StockMovement map(ResultSet rs) throws SQLException {
        StockMovement m = new StockMovement(rs.getInt("product_id"), rs.getInt("cell_id"), nullableInt(rs, "order_id"),
                StockMovement.Type.valueOf(rs.getString("type")), rs.getInt("quantity"), rs.getString("username"));
        m.setId(rs.getInt("id"));
        m.setCreatedAt(dateTime(rs, "created_at"));
        m.setProductName(rs.getString("product_name"));
        return m;
    }

    @Override
    protected PreparedStatement insert(Connection c, StockMovement m) throws SQLException {
        return prepare(c, "INSERT INTO stock_movements(product_id, cell_id, order_id, type, quantity, username, created_at) VALUES (?,?,?,?,?,?,?)",
                m.getProductId(), m.getCellId(), m.getOrderId(), m.getType(), m.getQuantity(), m.getUsername(), m.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, StockMovement m) {
        throw new UnsupportedOperationException("Stock movements cannot be changed");
    }

    public List<StockMovement> latest(int limit) {
        return query(select() + orderBy() + " LIMIT ?", limit);
    }

}
