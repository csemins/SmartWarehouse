package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.RackCell;
import bd.ac.kuet.swms.plc.Actuator;
import bd.ac.kuet.swms.plc.ControllerState;
import bd.ac.kuet.swms.plc.FactoryIoController;
import bd.ac.kuet.swms.plc.Sensor;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.plc.WarehouseJob;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.ui.StatusPill;
import bd.ac.kuet.swms.ui.UiThrottle;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class WarehouseView extends View {
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final String FIRST_FREE_CELL = "First free cell";

    private final FactoryIoController plc = ctx.warehouse.controller();
    private final RackCellTile[] tiles = new RackCellTile[RackCell.COUNT + 1];
    private final Label controllerState = new Label();
    private final Label controllerMessage = hint("");
    private final Button start = new Button("Start");
    private final Button stop = new Button("Stop");
    private final Button reset = new Button("Reset");
    private final ProgressBar jobProgress = new ProgressBar(0);
    private final Label jobLabel = new Label("No job running");
    private final ComboBox<Product> storeProduct = new ComboBox<>();
    private final ComboBox<String> targetCell = new ComboBox<>();
    private final CheckBox autoStore = new CheckBox("Store every arriving box automatically");
    private final ComboBox<Product> shipProduct = new ComboBox<>();
    private final Button shipSelectedCell = new Button("Ship selected cell");
    private final ListView<String> jobQueue = new ListView<>();
    private final ListView<String> eventLog = new ListView<>();
    private final Circle[] sensorLamps = new Circle[Sensor.COUNT];
    private final Circle[] coilLamps = new Circle[Actuator.COUNT];
    private final Label targetPosition = hint("");
    private final Consumer<Snapshot> plcListener = new UiThrottle<Snapshot>(this::showSnapshot)::submit;
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);
    private List<RackCell> cells = new ArrayList<>();
    private List<WarehouseJob> queued = new ArrayList<>();
    private RackCell selectedCell;

    private static final Color LAMP_OFF = Color.web("#94a3b8", 0.35);

    @Override public String title() { return "Warehouse"; }

    @Override
    protected Node build() {
        GridPane rackGrid = buildRackGrid();
        VBox rackCard = card("Rack", "54 cells, 9 columns x 6 levels", legend(), hint("Click a cell to use it as the target cell, or to ship the box in it."), rackGrid);
        VBox.setVgrow(rackGrid, Priority.ALWAYS);
        rackGrid.setMinHeight(300);

        VBox side = new VBox(16, buildCranePanel(), buildStorePanel(), buildShipPanel(), buildQueuePanel(), buildIoPanel(),
                card("Event log", eventLog));
        eventLog.setPrefHeight(150);
        ScrollPane sideScroll = scrolling(side);

        HBox layout = new HBox(16, rackCard, sideScroll);
        HBox.setHgrow(rackCard, Priority.ALWAYS);
        sideScroll.prefWidthProperty().bind(layout.widthProperty().multiply(0.36));
        sideScroll.setMinWidth(340);
        plc.subscribe(plcListener);
        ctx.warehouse.onChange(changeListener);
        showSnapshot(plc.snapshot());
        return layout;
    }

    private javafx.scene.layout.FlowPane legend() {
        return new javafx.scene.layout.FlowPane(16, 4, legendItem("rack-cell", "Empty"), legendItem("occupied", "Stored"), legendItem("busy", "Crane working"));
    }

    private HBox legendItem(String styleClass, String text) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("rack-cell", "legend-swatch", styleClass);
        swatch.setPrefSize(18, 14);
        swatch.setMinSize(18, 14);
        Label label = new Label(text);
        label.setMinWidth(Region.USE_PREF_SIZE);
        HBox item = new HBox(6, swatch, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private GridPane buildRackGrid() {
        GridPane rack = new GridPane();
        rack.setHgap(6);
        rack.setVgap(6);
        for (int c = 0; c < COLUMNS; c++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / COLUMNS);
            rack.getColumnConstraints().add(column);
        }
        for (int r = 0; r < ROWS; r++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / ROWS);
            rack.getRowConstraints().add(row);
        }
        for (int id = 1; id <= RackCell.COUNT; id++) {
            RackCellTile tile = new RackCellTile(id);
            tile.setOnMouseClicked(e -> selectCell(tile.getCellId()));
            tiles[id] = tile;
            // cell 1 is bottom left in factory io, so flip the rows
            rack.add(tile, (id - 1) % COLUMNS, ROWS - 1 - (id - 1) / COLUMNS);
        }
        return rack;
    }

    private VBox buildCranePanel() {
        start.getStyleClass().add("success");
        stop.getStyleClass().add("danger");
        start.setOnAction(e -> plc.start());
        stop.setOnAction(e -> plc.stop());
        reset.setOnAction(e -> plc.reset());
        jobProgress.setMaxWidth(Double.MAX_VALUE);
        controllerMessage.setMinHeight(36);
        controllerMessage.setPrefHeight(36);
        controllerMessage.setMaxHeight(36);
        jobLabel.setMinHeight(18);
        jobLabel.setMaxWidth(Double.MAX_VALUE);
        HBox stateRow = new HBox(10, new Label("Controller"), controllerState);
        stateRow.setAlignment(Pos.CENTER_LEFT);
        return card("Crane", stateRow, controllerMessage, new HBox(8, start, stop, reset), jobLabel, jobProgress);
    }

    private VBox buildStorePanel() {
        storeProduct.setPromptText("Product in the arriving box");
        storeProduct.setMaxWidth(Double.MAX_VALUE);
        targetCell.setId("targetCell");
        targetCell.setMaxWidth(Double.MAX_VALUE);
        targetCell.getItems().add(FIRST_FREE_CELL);
        targetCell.setValue(FIRST_FREE_CELL);
        Button store = new Button("Store box");
        store.getStyleClass().add("primary");
        store.disableProperty().bind(storeProduct.valueProperty().isNull());
        store.setOnAction(e -> runJobRequest(() -> ctx.warehouse.requestStore(storeProduct.getValue(), chosenTargetCell())));
        autoStore.disableProperty().bind(storeProduct.valueProperty().isNull());
        autoStore.selectedProperty().addListener((o, old, on) -> updateAutoStore());
        storeProduct.valueProperty().addListener((o, old, p) -> updateAutoStore());
        targetCell.valueProperty().addListener((o, old, v) -> updateAutoStore());
        GridPane form = formGrid();
        form.addRow(0, new Label("Product"), storeProduct);
        form.addRow(1, new Label("Target cell"), targetCell);
        return card("Store incoming box", form, store, autoStore, hint("The crane takes the box from the load conveyor into the target cell."));
    }

    private VBox buildShipPanel() {
        shipProduct.setPromptText("Product to ship");
        shipProduct.setMaxWidth(Double.MAX_VALUE);
        Button shipOldest = new Button("Ship oldest box");
        shipOldest.getStyleClass().add("primary");
        shipOldest.disableProperty().bind(shipProduct.valueProperty().isNull());
        shipOldest.setOnAction(e -> runJobRequest(() -> ctx.warehouse.requestRetrieveOldest(shipProduct.getValue())));
        shipSelectedCell.setDisable(true);
        shipSelectedCell.setOnAction(e -> runJobRequest(() -> ctx.warehouse.requestRetrieve(selectedCell.getId())));
        return card("Ship box", shipProduct, new HBox(8, shipOldest, shipSelectedCell), hint("Orders from the Orders tab are shipped automatically."));
    }

    private VBox buildQueuePanel() {
        jobQueue.setPrefHeight(100);
        jobQueue.setPlaceholder(new Label("No jobs waiting"));
        Button cancel = new Button("Cancel selected job");
        cancel.setOnAction(e -> {
            int index = jobQueue.getSelectionModel().getSelectedIndex() - (plc.snapshot() != null && plc.snapshot().currentJob() != null ? 1 : 0);
            if (index >= 0 && index < queued.size()) plc.cancel(queued.get(index));
        });
        return card("Job queue", jobQueue, cancel);
    }

    private VBox buildIoPanel() {
        TilePane sensors = new TilePane(8, 4);
        for (Sensor sensor : Sensor.values()) {
            sensorLamps[sensor.address()] = new Circle(5, LAMP_OFF);
            sensors.getChildren().add(lamp(sensorLamps[sensor.address()], sensor.label()));
        }
        TilePane coils = new TilePane(8, 4);
        for (Actuator actuator : Actuator.values()) {
            coilLamps[actuator.address()] = new Circle(5, LAMP_OFF);
            coils.getChildren().add(lamp(coilLamps[actuator.address()], actuator.label()));
        }
        sensors.setPrefColumns(2);
        coils.setPrefColumns(2);
        return card("Live I/O", hint("Sensors (discrete inputs)"), sensors, hint("Actuators (coils)"), coils, targetPosition);
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints(80);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    private static HBox lamp(Circle circle, String text) {
        HBox box = new HBox(6, circle, new Label(text));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(150);
        return box;
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

    private void selectCell(int cellId) {
        selectedCell = cells.stream().filter(c -> c.getId() == cellId).findFirst().orElse(null);
        for (int id = 1; id <= RackCell.COUNT; id++) tiles[id].setSelected(id == cellId);
        boolean occupied = selectedCell != null && selectedCell.getStatus() == RackCell.Status.OCCUPIED;
        shipSelectedCell.setDisable(!occupied);
        shipSelectedCell.setText("Ship cell " + cellId);
        if (selectedCell != null && selectedCell.getStatus() == RackCell.Status.EMPTY) targetCell.setValue("Cell " + cellId);
    }

    @Override
    public void refresh() {
        FxAsync.run(() -> new Object[]{ctx.warehouse.cells(), ctx.products.findAll()}, data -> {
            @SuppressWarnings("unchecked") List<RackCell> loadedCells = (List<RackCell>) data[0];
            @SuppressWarnings("unchecked") List<Product> products = (List<Product>) data[1];
            cells = loadedCells;
            cells.forEach(cell -> tiles[cell.getId()].show(cell));
            String target = targetCell.getValue();
            List<String> targets = new ArrayList<>(List.of(FIRST_FREE_CELL));
            cells.stream().filter(c -> c.getStatus() == RackCell.Status.EMPTY).forEach(c -> targets.add("Cell " + c.getId()));
            targetCell.getItems().setAll(targets);
            targetCell.setValue(targets.contains(target) ? target : FIRST_FREE_CELL);
            Product current = storeProduct.getValue();
            storeProduct.getItems().setAll(products);
            if (current != null) products.stream().filter(p -> p.getId() == current.getId()).findFirst().ifPresent(storeProduct::setValue);
            shipProduct.getItems().setAll(products.stream().filter(p -> p.getBoxes() > 0).toList());
            if (selectedCell != null) selectCell(selectedCell.getId());
        });
    }

    private void showSnapshot(Snapshot s) {
        ControllerState state = s == null ? ControllerState.DISCONNECTED : s.state();
        StatusPill.show(controllerState, state);
        controllerMessage.setText(state == ControllerState.DISCONNECTED
                ? "Not connected. Connect to Factory IO in Settings."
                : s.message() + (ctx.automation.lastMessage().isEmpty() ? "" : "\n" + ctx.automation.lastMessage()));
        start.setDisable(!(state == ControllerState.IDLE || state == ControllerState.PAUSED));
        stop.setDisable(state != ControllerState.RUNNING);
        reset.setDisable(state == ControllerState.DISCONNECTED);
        if (autoStore.isSelected() && ctx.automation.autoStoreProduct() == null) autoStore.setSelected(false);
        if (s == null) return;

        WarehouseJob job = s.currentJob();
        jobLabel.setText(job == null ? "No job running" : job + "  -  " + job.progress());
        jobProgress.setProgress(job == null ? 0 : (double) job.getStepIndex() / job.getStepCount());
        for (Sensor sensor : Sensor.values()) sensorLamps[sensor.address()].setFill(s.input(sensor) ? Color.web("#22c55e") : LAMP_OFF);
        for (Actuator actuator : Actuator.values()) coilLamps[actuator.address()].setFill(s.coil(actuator) ? Color.web("#f59e0b") : LAMP_OFF);
        targetPosition.setText("Target position register: " + s.targetPosition());
        queued = s.queue();
        List<String> lines = new ArrayList<>();
        if (job != null) lines.add("Running: " + job);
        queued.forEach(j -> lines.add("Waiting: " + j));
        if (!lines.equals(jobQueue.getItems())) jobQueue.getItems().setAll(lines);
        List<String> events = plc.log();
        if (!events.equals(eventLog.getItems())) eventLog.getItems().setAll(events);
    }

    @Override
    public void dispose() {
        plc.unsubscribe(plcListener);
        ctx.warehouse.removeOnChange(changeListener);
    }
}
