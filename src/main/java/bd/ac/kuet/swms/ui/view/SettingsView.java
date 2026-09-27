package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.plc.Actuator;
import bd.ac.kuet.swms.plc.ControllerState;
import bd.ac.kuet.swms.plc.Side;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.service.AppSettings;
import bd.ac.kuet.swms.ui.Dialogs;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.ui.StatusPill;
import bd.ac.kuet.swms.ui.UiThrottle;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SettingsView extends View {
    private final TextField host = new TextField();
    private final Spinner<Integer> port = new Spinner<>(1, 65535, 502);
    private final Spinner<Integer> slaveId = new Spinner<>(0, 255, 1);
    private final CheckBox autoConnect = new CheckBox("Connect to Factory IO when the application starts");
    private final CheckBox autoStart = new CheckBox("Start automatic mode after connecting");
    private final ChoiceBox<Side> loadSide = sideChoice();
    private final ChoiceBox<Side> unloadSide = sideChoice();
    private final ChoiceBox<Side> rackSide = sideChoice();
    private final CheckBox sensorsInverted = new CheckBox("Conveyor sensors are TRUE when nothing is in front of them");
    private final Slider stepTimeout = new Slider(10, 180, 60);
    private final Label connectionState = new Label();
    private final Label connectionMessage = hint("");
    private final Button connect = new Button("Save and connect");
    private final Button disconnect = new Button("Disconnect");
    private final Consumer<Snapshot> plcListener = new UiThrottle<Snapshot>(this::showConnection)::submit;

    @Override public String title() { return "Settings"; }

    private static ChoiceBox<Side> sideChoice() {
        return new ChoiceBox<>(FXCollections.observableArrayList(Side.LEFT, Side.RIGHT));
    }

    @Override
    protected Node build() {
        port.setEditable(true);
        slaveId.setEditable(true);
        GridPane connectionForm = formGrid();
        connectionForm.addRow(0, new Label("Host / IP"), host);
        connectionForm.addRow(1, new Label("Port"), port);
        connectionForm.addRow(2, new Label("Slave ID"), slaveId);
        connect.getStyleClass().add("primary");
        connect.setOnAction(e -> saveSettings(true));
        disconnect.setOnAction(e -> ctx.warehouse.disconnect());
        HBox stateRow = new HBox(10, new Label("Status"), connectionState);
        stateRow.setAlignment(Pos.CENTER_LEFT);
        VBox connectionCard = card("Factory IO connection", "Modbus TCP/IP Server driver in Factory IO", connectionForm, autoConnect, autoStart,
                new HBox(8, connect, disconnect), stateRow, connectionMessage,
                hint("Use the IP shown in Factory IO under Drivers > Modbus TCP/IP Server."));

        stepTimeout.setShowTickLabels(true);
        stepTimeout.setMajorTickUnit(30);
        Label timeoutValue = new Label();
        timeoutValue.textProperty().bind(stepTimeout.valueProperty().asString("%.0f s"));
        GridPane scene = formGrid();
        scene.addRow(0, new Label("Load conveyor side"), loadSide);
        scene.addRow(1, new Label("Unload conveyor side"), unloadSide);
        scene.addRow(2, new Label("Rack side"), rackSide);
        scene.addRow(3, new Label("Step timeout"), new HBox(8, stepTimeout, timeoutValue));
        scene.add(sensorsInverted, 0, 4, 2, 1);
        Button saveScene = new Button("Save scene settings");
        saveScene.setOnAction(e -> saveSettings(false));

        FlowPane coilButtons = new FlowPane(6, 6);
        for (Actuator actuator : Actuator.values()) {
            ToggleButton toggle = new ToggleButton(actuator.label());
            toggle.setOnAction(e -> ctx.warehouse.controller().setCoilManually(actuator, toggle.isSelected()));
            coilButtons.getChildren().add(toggle);
        }
        Spinner<Integer> target = new Spinner<>(0, 55, 55);
        target.setEditable(true);
        Button move = new Button("Move crane");
        move.setOnAction(e -> ctx.warehouse.controller().setTargetManually(target.getValue()));
        VBox manual = new VBox(8, hint("Works while connected and stopped (IDLE)."), coilButtons,
                new HBox(8, new Label("Target position (1-54, 55 = rest)"), target, move));

        Spinner<Integer> cell = new Spinner<>(1, 54, 1);
        cell.setEditable(true);
        Button markEmpty = new Button("Mark cell as empty");
        markEmpty.setOnAction(e -> {
            int id = cell.getValue();
            if (Dialogs.confirm("Mark cell " + id + " as empty? Only do this if the box is gone in Factory IO."))
                FxAsync.run(() -> { ctx.warehouse.markCellEmpty(id); return true; }, ok -> Dialogs.info("Done", "Cell " + id + " is empty."));
        });
        VBox correction = new VBox(8, hint("After restarting the Factory IO scene its rack is empty; mark the cells here."),
                new HBox(8, new Label("Cell"), cell, markEmpty));

        Accordion advanced = new Accordion(
                new TitledPane("Automated Warehouse scene", new VBox(10, scene, saveScene)),
                new TitledPane("Manual crane control", manual),
                new TitledPane("Rack correction", correction));
        VBox content = new VBox(16, connectionCard, card("Advanced", "Scene wiring, manual control and rack correction", advanced));
        ctx.warehouse.controller().subscribe(plcListener);
        showConnection(ctx.warehouse.controller().snapshot());
        ScrollPane scroll = scrolling(content);
        return scroll;
    }

    private void showConnection(Snapshot s) {
        ControllerState state = s == null ? ControllerState.DISCONNECTED : s.state();
        StatusPill.show(connectionState, state);
        connectionMessage.setText(s == null ? "Not connected" : s.endpoint() + "  -  " + s.message());
        disconnect.setDisable(s == null || state == ControllerState.DISCONNECTED && s.endpoint().equals("-"));
    }

    private void saveSettings(boolean connectAfterwards) {
        AppSettings settings = new AppSettings();
        settings.factoryIoHost = host.getText().trim();
        settings.factoryIoPort = port.getValue();
        settings.slaveId = slaveId.getValue();
        settings.autoConnect = autoConnect.isSelected();
        settings.autoStart = autoStart.isSelected();
        settings.loadSide = loadSide.getValue();
        settings.unloadSide = unloadSide.getValue();
        settings.rackSide = rackSide.getValue();
        settings.conveyorSensorsInverted = sensorsInverted.isSelected();
        settings.stepTimeoutSeconds = (int) stepTimeout.getValue();
        settings.darkMode = ctx.warehouse.settings().darkMode;
        FxAsync.run(() -> {
            ctx.warehouse.saveSettings(settings);
            if (connectAfterwards) {
                ctx.warehouse.connect();
                if (settings.autoStart) ctx.warehouse.controller().startWhenConnected();
            }
            return true;
        }, ok -> { if (!connectAfterwards) Dialogs.info("Saved", "Settings saved to settings.json."); });
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints(150);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMaxWidth(320);
        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    @Override
    public void refresh() {
        AppSettings s = ctx.warehouse.settings();
        host.setText(s.factoryIoHost);
        port.getValueFactory().setValue(s.factoryIoPort);
        slaveId.getValueFactory().setValue(s.slaveId);
        autoConnect.setSelected(s.autoConnect);
        autoStart.setSelected(s.autoStart);
        loadSide.setValue(s.loadSide);
        unloadSide.setValue(s.unloadSide);
        rackSide.setValue(s.rackSide);
        sensorsInverted.setSelected(s.conveyorSensorsInverted);
        stepTimeout.setValue(s.stepTimeoutSeconds);
    }

    @Override
    public void dispose() { ctx.warehouse.controller().unsubscribe(plcListener); }
}
