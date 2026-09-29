package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.BaseEntity;
import bd.ac.kuet.swms.ui.Dialogs;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.ui.TableExport;
import bd.ac.kuet.swms.ui.Icons;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

// table on the left, edit form on the right. products/orders/transactions use this
public abstract class TableFormView<T extends BaseEntity> extends View {
    protected final TableView<T> table = new TableView<>();
    protected final GridPane form = new GridPane();
    protected final HBox extraButtons = new HBox(8);
    protected final VBox sidePanel = new VBox(16);
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final FilteredList<T> visibleItems = new FilteredList<>(items);
    private final TextField search = new TextField();
    private final Label formTitle = new Label();
    protected T selected;

    protected abstract String itemName();

    protected abstract void addColumns();

    protected abstract void addFormFields();

    protected abstract void showInForm(T item);

    protected abstract T readForm(T selected) throws Exception;

    protected abstract List<T> loadItems() throws Exception;

    protected abstract void saveItem(T item) throws Exception;

    protected abstract void deleteItem(T item) throws Exception;

    protected abstract String searchText(T item);

    @Override
    protected Node build() {
        formTitle.getStyleClass().add("card-title");
        search.setPromptText("Search " + itemName() + "s...");
        search.textProperty().addListener((o, old, text) -> visibleItems.setPredicate(
                item -> text.isBlank() || searchText(item).toLowerCase().contains(text.toLowerCase())));
        table.setItems(visibleItems);
        table.setPlaceholder(emptyState("clipboard", "Nothing here yet", "Click New " + itemName() + " to add the first one."));
        table.getSelectionModel().selectedItemProperty().addListener((o, old, item) -> { if (item != null) edit(item); });
        addColumns();
        Button export = new Button("Export CSV", Icons.icon("download", 14));
        export.setOnAction(e -> TableExport.exportToCsv(table, itemName() + "s.csv"));
        Button newButton = new Button("New " + itemName());
        newButton.getStyleClass().add("primary");
        newButton.setOnAction(e -> edit(null));
        Label count = new Label();
        count.getStyleClass().add("hint");
        count.textProperty().bind(Bindings.size(visibleItems).asString("%d shown"));
        HBox toolbar = new HBox(8, search, count, export, newButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox left = card(title(), toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        form.setVgap(4);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().add(fieldColumn);
        addFormFields();

        Button saveButton = new Button("Save");
        Button deleteButton = new Button("Delete");
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(e -> onSave());
        deleteButton.setOnAction(e -> onDelete());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        saveButton.getStyleClass().add("primary");
        deleteButton.getStyleClass().add("soft-danger");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox formButtons = new HBox(8, deleteButton, spacer, extraButtons, saveButton);
        VBox formCard = new VBox(14, formTitle, form, formButtons);
        formCard.getStyleClass().add("card");
        sidePanel.getChildren().add(0, formCard);
        ScrollPane side = scrolling(sidePanel);
        side.setMinWidth(320);
        HBox layout = new HBox(16, left, side);
        HBox.setHgrow(left, Priority.ALWAYS);
        side.setPrefWidth(360);
        side.setMaxWidth(440);
        edit(null);
        return layout;
    }

    public void search(String text) { search.setText(text); }

    protected void addFormRow(String label, Node field) {
        int row = form.getRowCount();
        if (!label.isEmpty()) {
            Label caption = new Label(label);
            caption.getStyleClass().add("field-label");
            caption.setPadding(new Insets(row == 0 ? 0 : 8, 0, 0, 0));
            form.add(caption, 0, row++);
        }
        form.add(field, 0, row);
        if (field instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public void refresh() {
        FxAsync.run(this::loadItems, list -> {
            int selectedId = selected == null ? 0 : selected.getId();
            items.setAll(list);
            items.stream().filter(i -> i.getId() == selectedId).findFirst().ifPresent(i -> table.getSelectionModel().select(i));
        });
    }

    protected void edit(T item) {
        selected = item;
        if (item == null) table.getSelectionModel().clearSelection();
        formTitle.setText(item == null ? "New " + itemName() : item.getDisplayName());
        showInForm(item);
    }

    private void onSave() {
        T item;
        try {
            item = readForm(selected);
            item.validate();
        } catch (Exception e) {
            Dialogs.error("Please check the form", e.getMessage());
            return;
        }
        FxAsync.run(() -> { saveItem(item); return item; }, saved -> {
            selected = saved;
            refresh();
        });
    }

    private void onDelete() {
        T item = table.getSelectionModel().getSelectedItem();
        if (item == null || !Dialogs.confirm("Delete " + item.getDisplayName() + "?")) return;
        FxAsync.run(() -> { deleteItem(item); return true; }, ok -> {
            edit(null);
            refresh();
        });
    }

    protected static double parseNumber(TextField field, String name) throws Exception {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new Exception(name + " must be a number");
        }
    }
}
