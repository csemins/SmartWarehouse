package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Category;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.ui.Dialogs;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;

import java.util.List;

public class ProductsView extends TableFormView<Product> {
    private final TextField sku = new TextField();
    private final TextField name = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField costPrice = new TextField();
    private final TextField unitPrice = new TextField();
    private final Spinner<Integer> unitsPerBox = new Spinner<>(1, 500, 1);
    private final Spinner<Integer> reorderLevel = new Spinner<>(0, 100_000, 0);

    @Override public String title() { return "Products"; }

    @Override protected String itemName() { return "product"; }

    @Override
    protected void addColumns() {
        addColumn(table, "SKU", Product::getSku, 0.15);
        addColumn(table, "Name", Product::getName, 0.25);
        addColumn(table, "Category", Product::getCategoryName, 0.15);
        addColumn(table, "Boxes", Product::getBoxes, 0.09);
        addColumn(table, "Units", Product::getQuantity, 0.09);
        addColumn(table, "Cost", p -> Formats.money(p.getCostPrice()), 0.12);
        addColumn(table, "Price", p -> Formats.money(p.getUnitPrice()), 0.12);
        table.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().remove("row-warning");
                if (!empty && p != null && p.needsRestock()) getStyleClass().add("row-warning");
            }
        });
    }

    @Override
    protected void addFormFields() {
        category.setEditable(true);
        reorderLevel.setEditable(true);
        unitsPerBox.setEditable(true);
        addFormRow("SKU / barcode", sku);
        addFormRow("Name", name);
        addFormRow("Category", category);
        addFormRow("Cost price (Tk)", costPrice);
        addFormRow("Selling price (Tk)", unitPrice);
        addFormRow("Units per box", unitsPerBox);
        addFormRow("Reorder level (units)", reorderLevel);
    }

    @Override
    protected void showInForm(Product p) {
        sku.setText(p == null ? "" : p.getSku());
        name.setText(p == null ? "" : p.getName());
        category.getEditor().setText(p == null || p.getCategoryName() == null ? "" : p.getCategoryName());
        costPrice.setText(p == null ? "" : String.valueOf(p.getCostPrice()));
        unitPrice.setText(p == null ? "" : String.valueOf(p.getUnitPrice()));
        unitsPerBox.getValueFactory().setValue(p == null ? 1 : p.getUnitsPerBox());
        reorderLevel.getValueFactory().setValue(p == null ? 0 : p.getReorderLevel());
    }

    @Override
    protected Product readForm(Product selected) throws Exception {
        Product p = selected == null ? new Product() : selected;
        p.setSku(sku.getText().trim());
        p.setName(name.getText().trim());
        p.setCostPrice(parseNumber(costPrice, "Cost price"));
        p.setUnitPrice(parseNumber(unitPrice, "Selling price"));
        p.setUnitsPerBox(unitsPerBox.getValue());
        p.setReorderLevel(reorderLevel.getValue());
        return p;
    }

    @Override
    protected List<Product> loadItems() {
        List<String> names = ctx.products.categories().stream().map(Category::getName).toList();
        Platform.runLater(() -> category.getItems().setAll(names));
        return ctx.products.findAll();
    }

    @Override protected void saveItem(Product p) { ctx.products.save(p, category.getEditor().getText()); }

    @Override protected void deleteItem(Product p) { ctx.products.delete(p); }

    @Override protected String searchText(Product p) { return p.getSku() + " " + p.getName() + " " + p.getCategoryName(); }
}
