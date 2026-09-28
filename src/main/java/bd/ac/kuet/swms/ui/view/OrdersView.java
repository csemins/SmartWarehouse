package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.ui.Toasts;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

import java.util.List;

public class OrdersView extends TableFormView<SalesOrder> {
    private final TextField customer = new TextField();
    private final ComboBox<Product> product = new ComboBox<>();
    private final Spinner<Integer> boxes = new Spinner<>(1, 54, 1);
    private final Label availability = new Label();
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);

    @Override public String title() { return "Orders"; }

    @Override protected String itemName() { return "order"; }

    @Override
    protected void addColumns() {
        addColumn(table, "#", SalesOrder::getId, 0.06);
        addColumn(table, "Date", o -> Formats.dateTime(o.getCreatedAt()), 0.18);
        addColumn(table, "Customer", SalesOrder::getCustomer, 0.22);
        addColumn(table, "Product", SalesOrder::getProductName, 0.26);
        addColumn(table, "Shipped", o -> o.getBoxesShipped() + " / " + o.getBoxes(), 0.12);
        showAsBadge(addColumn(table, "Status", SalesOrder::getStatus, 0.14), status -> switch (status) {
            case OPEN -> "badge-warning";
            case DONE -> "badge-success";
            case CANCELLED -> "badge-neutral";
        });
        ctx.warehouse.onChange(changeListener);
    }

    @Override
    protected void addFormFields() {
        customer.setId("orderCustomer");
        product.setId("orderProduct");
        product.setMaxWidth(Double.MAX_VALUE);
        boxes.setEditable(true);
        product.valueProperty().addListener((o, old, p) -> availability.setText(p == null ? "" : p.getBoxes() + " box(es) in the rack"));
        addFormRow("Customer", customer);
        addFormRow("Product", product);
        addFormRow("Boxes", boxes);
        addFormRow("", availability);
        Label hint = hint("Open orders are shipped automatically: the crane takes the oldest box of the product "
                + "to the exit as soon as it is free. Missing boxes are shipped when they arrive.");
        Button cancel = new Button("Cancel order");
        cancel.getStyleClass().add("soft-danger");
        cancel.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        cancel.setOnAction(e -> FxAsync.run(() -> { ctx.orders.cancel(table.getSelectionModel().getSelectedItem()); return true; }, ok -> refresh()));
        extraButtons.getChildren().addAll(cancel);
        form.add(hint, 0, form.getRowCount());
    }

    @Override
    protected void showInForm(SalesOrder o) {
        customer.setText(o == null ? "" : o.getCustomer());
        product.setValue(o == null ? null : product.getItems().stream().filter(p -> p.getId() == o.getProductId()).findFirst().orElse(null));
        boxes.getValueFactory().setValue(o == null ? 1 : o.getBoxes());
    }

    @Override
    protected SalesOrder readForm(SalesOrder selected) throws Exception {
        if (selected != null) throw new Exception("Orders cannot be changed. Cancel it and create a new one.");
        SalesOrder order = new SalesOrder();
        order.setCustomer(customer.getText().trim());
        order.setProductId(product.getValue() == null ? 0 : product.getValue().getId());
        order.setBoxes(boxes.getValue());
        return order;
    }

    @Override
    protected List<SalesOrder> loadItems() {
        List<Product> products = ctx.products.findAll();
        Platform.runLater(() -> {
            Product current = product.getValue();
            product.getItems().setAll(products);
            if (current != null) products.stream().filter(p -> p.getId() == current.getId()).findFirst().ifPresent(product::setValue);
        });
        return ctx.orders.findAll();
    }

    @Override
    protected void saveItem(SalesOrder order) {
        SalesOrder saved = ctx.orders.create(order.getCustomer(), product.getValue(), order.getBoxes());
        order.setId(saved.getId());
        Platform.runLater(() -> Toasts.show("Order #" + saved.getId() + " created, it will be shipped automatically"));
    }

    @Override protected void deleteItem(SalesOrder order) { ctx.orders.delete(order); }

    @Override protected String searchText(SalesOrder o) { return o.getCustomer() + " " + o.getProductName() + " " + o.getStatus(); }

    @Override public void dispose() { ctx.warehouse.removeOnChange(changeListener); }
}
