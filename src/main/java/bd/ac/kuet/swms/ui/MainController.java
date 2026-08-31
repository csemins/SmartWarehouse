package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.AppContext;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.ui.view.View;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MainController {
    @FXML private TabPane tabs;
    @FXML private Label userLabel, jobStatus;

    private final AppContext ctx = AppContext.get();
    private final List<View> views = new ArrayList<>();

    private record Module(Permission permission, Supplier<View> create) { }

    // TODO sidebar instead of tabs later, tabs are fine for now
    private static List<Module> modules() {
        List<Module> list = new ArrayList<>();
        return list;
    }

    @FXML
    private void initialize() {
        User user = ctx.session.user();
        userLabel.setText(user.getFullName() + "  (" + user.getRole() + ")");
        for (Module module : modules()) {
            if (!user.can(module.permission())) continue;
            View view = module.create().get();
            Tab tab = new Tab(view.title(), view.root());
            tab.setClosable(false);
            tab.setOnSelectionChanged(e -> { if (tab.isSelected()) view.refresh(); });
            tabs.getTabs().add(tab);
            views.add(view);
        }
        if (!views.isEmpty()) views.get(0).refresh();
    }

    @FXML
    private void onLogout() {
        close();
        ctx.auth.logout();
        Navigator.get().showLogin();
    }

    @FXML
    private void onExit() {
        close();
        Platform.exit();
    }

    private void close() {
        views.forEach(View::dispose);
    }
}
