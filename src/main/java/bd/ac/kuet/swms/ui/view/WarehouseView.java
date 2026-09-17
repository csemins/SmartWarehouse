package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.RackCell;
import bd.ac.kuet.swms.plc.FactoryIoController;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.plc.WarehouseJob;
import bd.ac.kuet.swms.ui.FxAsync;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class WarehouseView extends View {
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final String FIRST_FREE_CELL = "First free cell";

    private final FactoryIoController plc = ctx.warehouse.controller();
    private final Label[] cellLabels = new Label[RackCell.COUNT + 1];
    private final Label state = new Label("DISCONNECTED");
    private final Label message = new Label();
    private final Label jobLabel = new Label("No job running");
    private final ComboBox<Product> storeProduct = new ComboBox<>();
    private final ComboBox<String> targetCell = new ComboBox<>();
    private final CheckBox autoStore = new CheckBox("Store every arriving box automatically");
    private final ComboBox<Product> shipProduct = new ComboBox<>();
    private final ListView<String> eventLog = new ListView<>();
    private final Consumer<Snapshot> plcListener = s -> Platform.runLater(() -> showSnapshot(s));
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);

    @Override public String title() { return "Warehouse"; }

    @Override
    protected Node build() {
        GridPane rack = new GridPane();
        rack.setHgap(4);
        rack.setVgap(4);
        for (int id = 1; id <= RackCell.COUNT; id++) {
            Label cell = new Label(String.valueOf(id));
            cell.getStyleClass().add("rack-cell");
            cell.setMinSize(70, 44);
            cell.setWrapText(true);
            cellLabels[id] = cell;
            // cell 1 is bottom left in factory io
            rack.add(cell, (id - 1) % COLUMNS, ROWS - 1 - (id - 1) / COLUMNS);
        }

        Button start = new Button("Start");
        Button stop = new Button("Stop");
        Button reset = new Button("Reset");
        start.setOnAction(e -> plc.start());
        stop.setOnAction(e -> plc.stop());
        reset.setOnAction(e -> plc.reset());
        VBox crane = card("Crane", new HBox(8, new Label("State:"), state), message, new HBox(8, start, stop, reset), jobLabel);

        storeProduct.setPromptText("Product in the arriving box");
        Button store = new Button("Store box");
        store.disableProperty().bind(storeProduct.valueProperty().isNull());
        targetCell.getItems().add(FIRST_FREE_CELL);
        targetCell.setValue(FIRST_FREE_CELL);
        store.setOnAction(e -> runJobRequest(() -> ctx.warehouse.requestStore(storeProduct.getValue(), chosenTargetCell())));
        targetCell.valueProperty().addListener((o, old, v) -> updateAutoStore());
        autoStore.disableProperty().bind(storeProduct.valueProperty().isNull());
        autoStore.selectedProperty().addListener((o, old, on) -> updateAutoStore());
        storeProduct.valueProperty().addListener((o, old, p) -> updateAutoStore());
        VBox storeCard = card("Store incoming box", storeProduct, targetCell, store, autoStore);

        shipProduct.setPromptText("Product to ship");
        Button ship = new Button("Ship oldest box");
        ship.disableProperty().bind(shipProduct.valueProperty().isNull());
        ship.setOnAction(e -> runJobRequest(() -> ctx.warehouse.requestRetrieveOldest(shipProduct.getValue())));
        VBox shipCard = card("Ship box", shipProduct, ship);

        eventLog.setPrefHeight(200);
        VBox side = new VBox(10, crane, storeCard, shipCard, card("Event log", eventLog));
        side.setPrefWidth(340);
        HBox layout = new HBox(12, card("Rack (54 cells)", rack), scrolling(side));
        HBox.setHgrow(layout.getChildren().get(0), Priority.ALWAYS);
        plc.subscribe(plcListener);
        ctx.warehouse.onChange(changeListener);
        return layout;
    }

    private Integer chosenTargetCell() {
        String value = targetCell.getValue();
        return value == null || value.equals(FIRST_FREE_CELL) ? null : Integer.parseInt(value.replace("Cell ", ""));
    }

    private void updateAutoStore() {
        ctx.automation.setAutoStore(autoStore.isSelected() ? storeProduct.getValue() : null, chosenTargetCell());
    }

    private void runJobRequest(Callable<WarehouseJob> request) {
        FxAsync.run(request, job -> { });
    }

    @Override
    public void refresh() {
        FxAsync.run(() -> new Object[]{ctx.warehouse.cells(), ctx.products.findAll()}, data -> {
            @SuppressWarnings("unchecked") List<RackCell> cells = (List<RackCell>) data[0];
            @SuppressWarnings("unchecked") List<Product> products = (List<Product>) data[1];
            for (RackCell cell : cells) {
                Label label = cellLabels[cell.getId()];
                label.getStyleClass().removeAll("occupied", "busy");
                if (cell.getStatus() == RackCell.Status.EMPTY) label.setText(String.valueOf(cell.getId()));
                else {
                    label.setText(cell.getId() + "\n" + cell.getProductName());
                    label.getStyleClass().add(cell.getStatus() == RackCell.Status.OCCUPIED ? "occupied" : "busy");
                }
            }
            String target = targetCell.getValue();
            List<String> targets = new ArrayList<>(List.of(FIRST_FREE_CELL));
            cells.stream().filter(c -> c.getStatus() == RackCell.Status.EMPTY).forEach(c -> targets.add("Cell " + c.getId()));
            targetCell.getItems().setAll(targets);
            targetCell.setValue(targets.contains(target) ? target : FIRST_FREE_CELL);
            Product current = storeProduct.getValue();
            storeProduct.getItems().setAll(products);
            if (current != null) products.stream().filter(p -> p.getId() == current.getId()).findFirst().ifPresent(storeProduct::setValue);
            shipProduct.getItems().setAll(products.stream().filter(p -> p.getBoxes() > 0).toList());
        });
    }

    private void showSnapshot(Snapshot s) {
        if (s == null) return;
        state.setText(s.state().name());
        message.setText(s.message());
        WarehouseJob job = s.currentJob();
        jobLabel.setText(job == null ? "No job running" : job + "  -  " + job.progress());
        List<String> events = plc.log();
        if (!events.equals(eventLog.getItems())) eventLog.getItems().setAll(events);
    }

    @Override
    public void dispose() {
        plc.unsubscribe(plcListener);
        ctx.warehouse.removeOnChange(changeListener);
    }
}
