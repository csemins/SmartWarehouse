package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.FinancialTransaction;
import bd.ac.kuet.swms.util.Formats;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;

public class TransactionsView extends TableFormView<FinancialTransaction> {
    private final ComboBox<FinancialTransaction.Type> type = new ComboBox<>();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField amount = new TextField();
    private final DatePicker date = new DatePicker();
    private final TextArea description = new TextArea();

    @Override public String title() { return "Transactions"; }

    @Override protected String itemName() { return "transaction"; }

    @Override
    protected void addColumns() {
        addColumn(table, "Date", FinancialTransaction::getDate, 0.14);
        showAsBadge(addColumn(table, "Type", FinancialTransaction::getType, 0.12),
                type -> type == FinancialTransaction.Type.INCOME ? "badge-success" : "badge-danger");
        addColumn(table, "Category", FinancialTransaction::getCategory, 0.16);
        addColumn(table, "Amount", t -> Formats.money(t.getAmount()), 0.18);
        addColumn(table, "Description", FinancialTransaction::getDescription, 0.40);
    }

    @Override
    protected void addFormFields() {
        type.getItems().setAll(FinancialTransaction.Type.values());
        category.setEditable(true);
        category.getItems().setAll("Sales", "Purchase", "Salaries", "Rent", "Utilities", "Transport", "Maintenance", "Other");
        description.setPrefRowCount(3);
        description.setWrapText(true);
        addFormRow("Type", type);
        addFormRow("Category", category);
        addFormRow("Amount (Tk)", amount);
        addFormRow("Date", date);
        addFormRow("Description", description);
    }

    @Override
    protected void showInForm(FinancialTransaction t) {
        type.setValue(t == null ? FinancialTransaction.Type.EXPENSE : t.getType());
        category.getEditor().setText(t == null ? "" : t.getCategory());
        amount.setText(t == null ? "" : String.valueOf(t.getAmount()));
        date.setValue(t == null ? LocalDate.now() : t.getDate());
        description.setText(t == null ? "" : t.getDescription());
    }

    @Override
    protected FinancialTransaction readForm(FinancialTransaction selected) throws Exception {
        FinancialTransaction t = selected == null ? new FinancialTransaction() : selected;
        t.setType(type.getValue());
        t.setCategory(category.getEditor().getText().trim());
        t.setAmount(parseNumber(amount, "Amount"));
        t.setDate(date.getValue());
        t.setDescription(description.getText().trim());
        return t;
    }

    @Override protected List<FinancialTransaction> loadItems() { return ctx.finance.findAll(); }

    @Override protected void saveItem(FinancialTransaction t) { ctx.finance.save(t); }

    @Override protected void deleteItem(FinancialTransaction t) { ctx.finance.delete(t); }

    @Override protected String searchText(FinancialTransaction t) { return t.getCategory() + " " + t.getDescription() + " " + t.getType(); }
}
