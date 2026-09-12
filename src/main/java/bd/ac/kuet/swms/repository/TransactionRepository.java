package bd.ac.kuet.swms.repository;

import bd.ac.kuet.swms.db.DataAccessException;
import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.FinancialTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

public class TransactionRepository extends AbstractRepository<FinancialTransaction> {
    public TransactionRepository(Database db) { super(db); }

    @Override protected String table() { return "transactions"; }

    @Override protected String orderBy() { return " ORDER BY t.tx_date DESC, t.id DESC"; }

    @Override
    protected FinancialTransaction map(ResultSet rs) throws SQLException {
        FinancialTransaction t = new FinancialTransaction(FinancialTransaction.Type.valueOf(rs.getString("type")),
                rs.getString("category"), rs.getDouble("amount"), rs.getString("description"));
        t.setId(rs.getInt("id"));
        t.setDate(LocalDate.parse(rs.getString("tx_date")));
        t.setMovementId(nullableInt(rs, "movement_id"));
        t.setUsername(rs.getString("username"));
        t.setCreatedAt(dateTime(rs, "created_at"));
        return t;
    }

    @Override
    protected PreparedStatement insert(Connection c, FinancialTransaction t) throws SQLException {
        return prepare(c, "INSERT INTO transactions(type, category, amount, description, tx_date, movement_id, username, created_at) VALUES (?,?,?,?,?,?,?,?)",
                t.getType(), t.getCategory(), t.getAmount(), t.getDescription(), t.getDate(), t.getMovementId(), t.getUsername(), t.getCreatedAt());
    }

    @Override
    protected PreparedStatement update(Connection c, FinancialTransaction t) throws SQLException {
        return prepare(c, "UPDATE transactions SET type=?, category=?, amount=?, description=?, tx_date=? WHERE id=?",
                t.getType(), t.getCategory(), t.getAmount(), t.getDescription(), t.getDate(), t.getId());
    }

    public double total(FinancialTransaction.Type type) {
        return number("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = ?", type.name());
    }

    // [0] income, [1] expense
    public Map<YearMonth, double[]> totalsPerMonth(int months) {
        Map<YearMonth, double[]> result = new LinkedHashMap<>();
        YearMonth first = YearMonth.now().minusMonths(months - 1L);
        for (int i = 0; i < months; i++) result.put(first.plusMonths(i), new double[2]);
        String sql = "SELECT substr(tx_date, 1, 7) AS month, type, SUM(amount) AS total FROM transactions WHERE tx_date >= ? GROUP BY month, type";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, first.atDay(1).toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double[] incomeExpense = result.get(YearMonth.parse(rs.getString("month")));
                    if (incomeExpense != null) incomeExpense["INCOME".equals(rs.getString("type")) ? 0 : 1] = rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
        return result;
    }
}
