package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.RackCell;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class RackCellRepository extends AbstractRepository<RackCell> {
    public RackCellRepository(Database db) { super(db); }

    @Override protected String table() { return "rack_cells"; }

    @Override
    protected String select() {
        return "SELECT t.*, p.name AS product_name, p.sku AS sku FROM rack_cells t LEFT JOIN products p ON p.id = t.product_id";
    }

    @Override
    protected RackCell map(ResultSet rs) throws SQLException {
        RackCell cell = new RackCell();
        cell.setId(rs.getInt("id"));
        cell.setStatus(RackCell.Status.valueOf(rs.getString("status")));
        cell.setProductId(nullableInt(rs, "product_id"));
        cell.setQuantity(rs.getInt("quantity"));
        cell.setUpdatedAt(dateTime(rs, "updated_at"));
        cell.setProductName(rs.getString("product_name"));
        cell.setSku(rs.getString("sku"));
        return cell;
    }

    @Override
    protected PreparedStatement insert(Connection c, RackCell cell) {
        throw new UnsupportedOperationException("The 54 rack cells are created by the schema");
    }

    @Override
    protected PreparedStatement update(Connection c, RackCell cell) throws SQLException {
        return prepare(c, "UPDATE rack_cells SET status=?, product_id=?, quantity=?, updated_at=? WHERE id=?",
                cell.getStatus(), cell.getProductId(), cell.getQuantity(), LocalDateTime.now(), cell.getId());
    }

    public Optional<RackCell> firstEmpty() {
        return query(select() + " WHERE t.status = 'EMPTY' ORDER BY t.id LIMIT 1").stream().findFirst();
    }

    // FIFO: oldest box leaves first
    public Optional<RackCell> oldestBoxOf(int productId) {
        return query(select() + " WHERE t.status = 'OCCUPIED' AND t.product_id = ? ORDER BY t.updated_at LIMIT 1", productId)
                .stream().findFirst();
    }

    public int countEmpty() {
        return (int) number("SELECT COUNT(*) FROM rack_cells WHERE status = 'EMPTY'");
    }

    public List<RackCell> occupied() {
        return query(select() + " WHERE t.status = 'OCCUPIED' ORDER BY t.id");
    }
}
