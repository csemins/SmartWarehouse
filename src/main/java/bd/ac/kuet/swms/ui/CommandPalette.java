package bd.ac.kuet.swms.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Supplier;

public class CommandPalette {
    public record Entry(String title, String subtitle, String icon, Runnable action) { }

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final TextField query = new TextField();
    private final ObservableList<Entry> entries = FXCollections.observableArrayList();
    private final FilteredList<Entry> matches = new FilteredList<>(entries);
    private final ListView<Entry> results = new ListView<>(matches);
    private final Supplier<List<Entry>> source;

    public CommandPalette(Window owner, Supplier<List<Entry>> source) {
        this.source = source;
        query.setId("paletteQuery");
        query.setPromptText("Search pages, products and orders...");
        query.getStyleClass().add("palette-query");
        query.textProperty().addListener((o, old, text) -> {
            String q = text.toLowerCase().trim();
            matches.setPredicate(e -> q.isEmpty() || (e.title() + " " + e.subtitle()).toLowerCase().contains(q));
            results.getSelectionModel().selectFirst();
        });
        results.setCellFactory(l -> new EntryCell());
        results.setPrefHeight(340);
        results.setPlaceholder(new Label("No results"));
        results.setOnMouseClicked(e -> runSelected());
        query.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) results.getSelectionModel().selectNext();
            else if (e.getCode() == KeyCode.UP) results.getSelectionModel().selectPrevious();
            else if (e.getCode() == KeyCode.ENTER) runSelected();
            else if (e.getCode() == KeyCode.ESCAPE) stage.hide();
            else return;
            e.consume();
        });
        Label hint = new Label("Enter to open   ·   Up / Down to move   ·   Esc to close");
        hint.getStyleClass().add("hint");
        VBox panel = new VBox(10, query, results, hint);
        panel.getStyleClass().add("palette");
        panel.setMaxSize(560, VBox.USE_PREF_SIZE);
        StackPane root = new StackPane(panel);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 24;");
        Scene scene = new Scene(root, 610, 480, Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initOwner(owner);
        // click outside closes it
        stage.focusedProperty().addListener((o, was, focused) -> { if (!focused) stage.hide(); });
    }

    public void show() {
        Scene scene = stage.getScene();
        scene.getStylesheets().setAll(stage.getOwner().getScene().getStylesheets());
        Theme.apply(scene.getRoot());
        entries.setAll(source.get());
        query.clear();
        results.getSelectionModel().selectFirst();
        Window owner = stage.getOwner();
        stage.setX(owner.getX() + (owner.getWidth() - 610) / 2);
        stage.setY(owner.getY() + 90);
        stage.show();
        query.requestFocus();
        Motion.enter(scene.getRoot(), 0, -12);
    }

    public boolean isShowing() { return stage.isShowing(); }

    private void runSelected() {
        Entry entry = results.getSelectionModel().getSelectedItem();
        if (entry == null) return;
        stage.hide();
        entry.action().run();
    }

    private static class EntryCell extends ListCell<Entry> {
        @Override
        protected void updateItem(Entry e, boolean empty) {
            super.updateItem(e, empty);
            setText(null);
            if (empty || e == null) { setGraphic(null); return; }
            Node icon = Icons.icon(e.icon(), 16);
            Label title = new Label(e.title());
            title.getStyleClass().add("strong");
            Label subtitle = new Label(e.subtitle());
            subtitle.getStyleClass().add("hint");
            HBox row = new HBox(12, icon, new VBox(1, title, subtitle));
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
        }
    }
}
