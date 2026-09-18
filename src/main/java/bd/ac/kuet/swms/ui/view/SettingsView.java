package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.plc.Actuator;
import bd.ac.kuet.swms.plc.Side;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.service.AppSettings;
import bd.ac.kuet.swms.ui.Dialogs;
import bd.ac.kuet.swms.ui.FxAsync;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SettingsView extends View {
    private final TextField host = new TextField();
    private final Spinner<Integer> port = new Spinner<>(1, 65535, 502);
    private final Spinner<Integer> slaveId = new Spinner<>(0, 255, 1);
    private final CheckBox autoConnect = new CheckBox("Connect to Factory IO when the application starts");
    private final CheckBox autoStart = new CheckBox("Start automatic mode after connecting");
    private final ChoiceBox<Side> loadSide = new ChoiceBox<>(FXCollections.observableArrayList(Side.LEFT, Side.RIGHT));
    private final ChoiceBox<Side> unloadSide = new ChoiceBox<>(FXCollections.observableArrayList(Side.LEFT, Side.RIGHT));
    private final ChoiceBox<Side> rackSide = new ChoiceBox<>(FXCollections.observableArrayList(Side.LEFT, Side.RIGHT));
    private final CheckBox sensorsInverted = new CheckBox("Conveyor sensors are TRUE when nothing is in front of them");
    private final Spinner<Integer> stepTimeout = new Spinner<>(10, 180, 60);
    private final Label connectionState = new Label();
    private final Consumer<Snapshot> plcListener = s -> Platform.runLater(() -> showConnection(s));

    @Override public String title() { return "Settings"; }

    @Override
    protected Node build() {
        port.setEditable(true);
        slaveId.setEditable(true);
        stepTimeout.setEditable(true);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Host / IP"), host);
        grid.addRow(1, new Label("Port"), port);
        grid.addRow(2, new Label("Slave ID"), slaveId);
        Button connect = new Button("Save and connect");
        connect.setOnAction(e -> saveSettings(true));
        Button disconnect = new Button("Disconnect");
        disconnect.setOnAction(e -> ctx.warehouse.disconnect());
        VBox connection = card("Factory IO connection", grid, new HBox(8, connect, disconnect), connectionState);
        connection.getChildren().add(2, new VBox(4, autoConnect, autoStart));

        GridPane scene = new GridPane();
        scene.setHgap(10);
        scene.setVgap(8);
        scene.addRow(0, new Label("Load conveyor side"), loadSide);
        scene.addRow(1, new Label("Unload conveyor side"), unloadSide);
        scene.addRow(2, new Label("Rack side"), rackSide);
        scene.addRow(3, new Label("Step timeout (s)"), stepTimeout);
        scene.add(sensorsInverted, 0, 4, 2, 1);
        Button saveScene = new Button("Save scene settings");
        saveScene.setOnAction(e -> saveSettings(false));
        VBox content = new VBox(12, connection, card("Automated Warehouse scene", scene, saveScene));

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
        content.getChildren().add(card("Manual crane control", hint("Only while connected and stopped (IDLE)."), coilButtons,
                new HBox(8, new Label("Target (1-54, 55 = rest)"), target, move)));

        Spinner<Integer> cell = new Spinner<>(1, 54, 1);
        cell.setEditable(true);
        Button markEmpty = new Button("Mark cell as empty");
        markEmpty.setOnAction(e -> {
            int id = cell.getValue();
            if (Dialogs.confirm("Mark cell " + id + " as empty?"))
                FxAsync.run(() -> { ctx.warehouse.markCellEmpty(id); return true; }, ok -> Dialogs.info("Done", "Cell " + id + " is empty."));
        });
        content.getChildren().add(card("Rack correction", hint("After restarting the Factory IO scene the rack is empty, fix the cells here."),
                new HBox(8, new Label("Cell"), cell, markEmpty)));
        ctx.warehouse.controller().subscribe(plcListener);
        return scrolling(content);
    }

    private void showConnection(Snapshot s) {
        connectionState.setText(s == null ? "Not connected" : s.state() + "  " + s.endpoint() + "  -  " + s.message());
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
        settings.stepTimeoutSeconds = stepTimeout.getValue();
        FxAsync.run(() -> {
            ctx.warehouse.saveSettings(settings);
            if (connectAfterwards) ctx.warehouse.connect();
            return true;
        }, ok -> { if (!connectAfterwards) Dialogs.info("Saved", "Settings saved."); });
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
        stepTimeout.getValueFactory().setValue(s.stepTimeoutSeconds);
        showConnection(ctx.warehouse.controller().snapshot());
    }

    @Override
    public void dispose() { ctx.warehouse.controller().unsubscribe(plcListener); }
}
