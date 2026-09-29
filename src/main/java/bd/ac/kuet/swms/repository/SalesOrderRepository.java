package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.SalesOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SalesOrderRepository extends AbstractRepository<SalesOrder> {
    public SalesOrderRepository(Database db) { super(db); }

    @Override protected String table() { return "sales_orders"; }

    @Override
    protected String select() {
        return "SELECT t.*, p.name AS product_name FROM sales_orders t JOIN products p ON p.id = t.product_id";
    }

    @Override protected String orderBy() { return " ORDER BY t.id DESC"; }

    @Override
    protected SalesOrder map(ResultSet rs) throws SQLException {
        SalesOrder o = new SalesOrder();
        o.setId(rs.getInt("id"));
        o.setCustomer(rs.getString("customer"));
        o.setProductId(rs.getInt("product_id"));
        o.setProductName(rs.getString("product_name"));
        o.setBoxes(rs.getInt("boxes"));
        o.setBoxesShipped(rs.getInt("boxes_shipped"));
        o.setStatus(SalesOrder.Status.valueOf(rs.getString("status")));
        o.setUsername(rs.getString("username"));
        o.setCreatedAt(dateTime(rs, "created_at"));
        return o;
    }

    @Override
    protected PreparedStatement insert(Connection c, SalesOrder o) throws SQLException {
        return prepare(c, "INSERT INTO sales_orders(customer, product_id, boxes, boxes_shipped, status, username, created_at) VALUES (?,?,?,?,?,?,?)",
                o.getCustomer(), o.getProductId(), o.getBoxes(), o.getBoxesShipped(), o.getStatus(), o.getUsername(), o.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, SalesOrder o) throws SQLException {
        return prepare(c, "UPDATE sales_orders SET customer=?, product_id=?, boxes=?, boxes_shipped=?, status=? WHERE id=?",
                o.getCustomer(), o.getProductId(), o.getBoxes(), o.getBoxesShipped(), o.getStatus(), o.getId());
    }

    public Map<SalesOrder.Status, Integer> countByStatus() {
        Map<SalesOrder.Status, Integer> counts = new EnumMap<>(SalesOrder.Status.class);
        for (SalesOrder.Status status : SalesOrder.Status.values())
            counts.put(status, (int) number("SELECT COUNT(*) FROM sales_orders WHERE status = ?", status));
        return counts;
    }

    public List<SalesOrder> openOrders() {
        return query(select() + " WHERE t.status = 'OPEN' ORDER BY t.id");
    }
}
