package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.AuditEntry;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ActivityView extends View {
    private final TableView<AuditEntry> table = new TableView<>();

    @Override public String title() { return "Audit log"; }

    @Override
    protected Node build() {
        addColumn(table, "Time", e -> Formats.dateTime(e.getCreatedAt()), 0.18);
        addColumn(table, "User", AuditEntry::getUsername, 0.14);
        addColumn(table, "Category", AuditEntry::getCategory, 0.14);
        addColumn(table, "Action", AuditEntry::getAction, 0.54);
        VBox box = card("Audit log", hint("Sign-ins and every change, newest first"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    @Override
    public void refresh() {
        FxAsync.run(() -> ctx.audit.latest(500), table.getItems()::setAll);
    }
}
