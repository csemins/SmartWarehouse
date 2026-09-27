package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.DataAccessException;
import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, Double> salesPerCategory(int days) {
        String sql = "SELECT COALESCE(c.name, 'Uncategorised') AS category_name, SUM(m.quantity * p.unit_price) AS sales "
                + "FROM stock_movements m JOIN products p ON p.id = m.product_id LEFT JOIN categories c ON c.id = p.category_id "
                + "WHERE m.type = 'OUT' AND m.created_at >= ? GROUP BY category_name ORDER BY sales DESC";
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().minusDays(days - 1L).toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("category_name"), rs.getDouble("sales"));
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
        return result;
    }

    public Map<String, Integer> shippedUnitsPerProduct(int days, int limit) {
        String sql = "SELECT p.name AS product, SUM(m.quantity) AS units FROM stock_movements m JOIN products p ON p.id = m.product_id "
                + "WHERE m.type = 'OUT' AND m.created_at >= ? GROUP BY p.id ORDER BY units DESC LIMIT ?";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().minusDays(days - 1L).toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("product"), rs.getInt("units"));
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
        return result;
    }

    // [0] = in, [1] = out. prefill all days so the chart has no gaps
    public Map<LocalDate, int[]> unitsPerDay(int days) {
        Map<LocalDate, int[]> result = new LinkedHashMap<>();
        LocalDate first = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) result.put(first.plusDays(i), new int[2]);
        String sql = "SELECT substr(created_at, 1, 10) AS day, type, SUM(quantity) AS units FROM stock_movements WHERE created_at >= ? GROUP BY day, type";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, first.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int[] inOut = result.get(LocalDate.parse(rs.getString("day")));
                    if (inOut != null) inOut["IN".equals(rs.getString("type")) ? 0 : 1] = rs.getInt("units");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
        return result;
    }
}
