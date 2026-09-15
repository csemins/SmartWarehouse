package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.AuditEntry;
import bd.ac.kuet.swms.model.FinancialTransaction;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.repository.ProductRepository;
import bd.ac.kuet.swms.repository.TransactionRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class FinanceService {
    private final TransactionRepository transactions;
    private final ProductRepository products;
    private final Session session;
    private final AuditService audit;

    public FinanceService(TransactionRepository transactions, ProductRepository products, Session session, AuditService audit) {
        this.audit = audit;
        this.transactions = transactions;
        this.products = products;
        this.session = session;
    }

    public List<FinancialTransaction> findAll() { return transactions.findAll(); }

    public FinancialTransaction save(FinancialTransaction transaction) {
        session.require(Permission.MANAGE_FINANCE);
        if (transaction.isNew()) transaction.setUsername(session.username());
        boolean created = transaction.isNew();
        FinancialTransaction saved = transactions.save(transaction);
        audit.record(AuditEntry.Category.FINANCE, (created ? "Added " : "Updated ") + saved.getType().name().toLowerCase() + " " + saved.getCategory() + " of Tk " + String.format("%,.2f", saved.getAmount()));
        return saved;
    }

    public void delete(FinancialTransaction transaction) {
        session.require(Permission.MANAGE_FINANCE);
        transactions.delete(transaction.getId());
        audit.record(AuditEntry.Category.FINANCE, "Deleted transaction #" + transaction.getId());
    }

    public Summary summary() {
        double stockValue = products.findAll().stream().mapToDouble(Product::getStockValue).sum();
        return new Summary(transactions.total(FinancialTransaction.Type.INCOME),
                transactions.total(FinancialTransaction.Type.EXPENSE), stockValue);
    }

    public Map<YearMonth, double[]> totalsPerMonth(int months) { return transactions.totalsPerMonth(months); }

    public record Summary(double income, double expense, double stockValue) {
        public double profit() { return income - expense; }
    }
}
