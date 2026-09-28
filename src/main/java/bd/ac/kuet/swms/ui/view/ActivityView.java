package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.AuditEntry;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Map;

public class ActivityView extends View {
    private static final Map<AuditEntry.Category, String> PICTURES = Map.of(
            AuditEntry.Category.SECURITY, "lock", AuditEntry.Category.PRODUCTS, "package", AuditEntry.Category.ORDERS, "cart",
            AuditEntry.Category.WAREHOUSE, "factory", AuditEntry.Category.FINANCE, "money", AuditEntry.Category.SYSTEM, "gear");

    private final ObservableList<AuditEntry> entries = FXCollections.observableArrayList();
    private final FilteredList<AuditEntry> visible = new FilteredList<>(entries);
    private final TextField search = new TextField();
    private final ToggleGroup filter = new ToggleGroup();
    private final Label count = hint("");

    @Override public String title() { return "Audit log"; }

    @Override
    protected Node build() {
        search.setPromptText("Search actions or users...");
        HBox chips = new HBox(6);
        chips.getStyleClass().add("segmented");
        ToggleButton all = chip("All", null);
        all.setSelected(true);
        chips.getChildren().add(all);
        for (AuditEntry.Category category : AuditEntry.Category.values())
            chips.getChildren().add(chip(category.name().charAt(0) + category.name().substring(1).toLowerCase(), category));
        filter.selectedToggleProperty().addListener((o, old, t) -> { if (t == null) old.setSelected(true); else applyFilter(); });
        search.textProperty().addListener((o, old, t) -> applyFilter());
        count.textProperty().bind(javafx.beans.binding.Bindings.size(visible).asString("%d entries"));

        ListView<AuditEntry> list = new ListView<>(visible);
        list.setCellFactory(l -> new EntryCell());
        list.setPlaceholder(emptyState("scroll", "Nothing logged yet", "Sign-ins and every change are recorded here."));
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox toolbar = new HBox(12, search, count);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox box = card("Audit log", "Who changed what and when, stored in the audit_log table", toolbar, chips, list);
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private ToggleButton chip(String text, AuditEntry.Category category) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(category);
        button.setToggleGroup(filter);
        button.getStyleClass().add("chip");
        return button;
    }

    private void applyFilter() {
        AuditEntry.Category category = filter.getSelectedToggle() == null ? null : (AuditEntry.Category) filter.getSelectedToggle().getUserData();
        String text = search.getText().toLowerCase();
        visible.setPredicate(e -> (category == null || e.getCategory() == category)
                && (text.isBlank() || (e.getAction() + " " + e.getUsername()).toLowerCase().contains(text)));
    }

    @Override
    public void refresh() { FxAsync.run(() -> ctx.audit.latest(500), entries::setAll); }

    private static class EntryCell extends ListCell<AuditEntry> {
        @Override
        protected void updateItem(AuditEntry e, boolean empty) {
            super.updateItem(e, empty);
            setText(null);
            if (empty || e == null) { setGraphic(null); return; }
            StackPane badge = new StackPane(bd.ac.kuet.swms.ui.Icons.picture(PICTURES.get(e.getCategory()), 24));
            badge.getStyleClass().add("timeline-dot");
            Label action = new Label(e.getAction());
            action.getStyleClass().add("strong");
            Label meta = hint(e.getUsername() + "  ·  " + Formats.timeAgo(e.getCreatedAt()) + "  ·  " + Formats.dateTime(e.getCreatedAt()));
            Label tag = new Label(e.getCategory().name());
            tag.getStyleClass().addAll("badge", "badge-neutral");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(12, badge, new VBox(2, action, meta), spacer, tag);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
        }
    }
}
