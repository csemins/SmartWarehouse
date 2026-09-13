package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.StockMovement;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.ui.TableExport;
import bd.ac.kuet.swms.util.Formats;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MovementsView extends View {
    private final ObservableList<StockMovement> movements = FXCollections.observableArrayList();
    private final TableView<StockMovement> table = new TableView<>();
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);

    @Override public String title() { return "Stock movements"; }

    @Override
    protected Node build() {
        FilteredList<StockMovement> visible = new FilteredList<>(movements);
        ToggleGroup filter = new ToggleGroup();
        RadioButton all = new RadioButton("All");
        RadioButton in = new RadioButton("Stored (IN)");
        RadioButton out = new RadioButton("Shipped (OUT)");
        for (RadioButton r : new RadioButton[]{all, in, out}) r.setToggleGroup(filter);
        all.setSelected(true);
        filter.selectedToggleProperty().addListener((o, old, t) -> visible.setPredicate(
                m -> t == all || (t == in) == (m.getType() == StockMovement.Type.IN)));
        table.setItems(visible);
        table.setPlaceholder(new Label("No movements yet"));
        addColumn(table, "Time", m -> Formats.dateTime(m.getCreatedAt()), 0.17);
        showAsBadge(addColumn(table, "Type", StockMovement::getType, 0.07),
                type -> type == StockMovement.Type.IN ? "badge-success" : "badge-warning");
        addColumn(table, "Product", StockMovement::getProductName, 0.3);
        addColumn(table, "Units", StockMovement::getQuantity, 0.08);
        addColumn(table, "Cell", StockMovement::getCellId, 0.08);
        addColumn(table, "Order", m -> m.getOrderId() == null ? "" : "#" + m.getOrderId(), 0.1);
        addColumn(table, "User", StockMovement::getUsername, 0.16);
        Button export = new Button("Export CSV...");
        export.setOnAction(e -> TableExport.exportToCsv(table, "stock-movements.csv"));
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(16, all, in, out, spacer, export);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox box = card("Stock ledger", "Every box stored or shipped by the crane, newest first", bar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        ctx.warehouse.onChange(changeListener);
        return box;
    }

    @Override
    public void refresh() {
        FxAsync.run(() -> ctx.warehouse.latestMovements(1000), movements::setAll);
    }

    @Override
    public void dispose() { ctx.warehouse.removeOnChange(changeListener); }
}
