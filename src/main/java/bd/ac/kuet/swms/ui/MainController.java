package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.AppContext;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.ui.view.ActivityView;
import bd.ac.kuet.swms.ui.view.DashboardView;
import bd.ac.kuet.swms.ui.view.FinanceView;
import bd.ac.kuet.swms.ui.view.MovementsView;
import bd.ac.kuet.swms.ui.view.OrdersView;
import bd.ac.kuet.swms.ui.view.ProductsView;
import bd.ac.kuet.swms.ui.view.SettingsView;
import bd.ac.kuet.swms.ui.view.View;
import bd.ac.kuet.swms.ui.view.WarehouseView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MainController {
    @FXML private TabPane tabs;
    @FXML private Label userLabel, jobStatus;
    @FXML private Label restockAlert;

    private final AppContext ctx = AppContext.get();
    private final List<View> views = new ArrayList<>();
    private final Consumer<Snapshot> plcListener = new UiThrottle<Snapshot>(this::showCraneJob)::submit;
    private final Runnable stockListener = () -> Platform.runLater(this::refreshRestockAlert);

    private record Module(Permission permission, Supplier<View> create) { }

    // TODO sidebar instead of tabs later, tabs are fine for now
    private static List<Module> modules() {
        List<Module> list = new ArrayList<>();
        list.add(new Module(Permission.VIEW_DASHBOARD, DashboardView::new));
        list.add(new Module(Permission.OPERATE_WAREHOUSE, WarehouseView::new));
        list.add(new Module(Permission.MANAGE_ORDERS, OrdersView::new));
        list.add(new Module(Permission.MANAGE_PRODUCTS, ProductsView::new));
        list.add(new Module(Permission.VIEW_MOVEMENTS, MovementsView::new));
        list.add(new Module(Permission.MANAGE_FINANCE, FinanceView::new));
        list.add(new Module(Permission.CONFIGURE_SYSTEM, ActivityView::new));
        list.add(new Module(Permission.CONFIGURE_SYSTEM, SettingsView::new));
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
        ctx.warehouse.controller().subscribe(plcListener);
        ctx.warehouse.onChange(stockListener);
        refreshRestockAlert();
    }

    private void showCraneJob(Snapshot s) {
        jobStatus.setText(s.currentJob() == null ? "Crane idle" : "Crane: " + s.currentJob() + "  -  " + s.currentJob().progress());
    }

    private void refreshRestockAlert() {
        FxAsync.run(() -> ctx.products.productsToRestock().size(), count -> {
            restockAlert.setText(count == 0 ? "" : count + " product(s) to restock");
            restockAlert.setVisible(count > 0);
        });
    }

    @FXML
    private void onExportReport() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("inventory-report.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON file", "*.json"));
        File file = chooser.showSaveDialog(Navigator.get().stage());
        if (file != null) FxAsync.run(() -> { ctx.reports.exportInventoryReport(file.toPath()); return file; },
                f -> Dialogs.info("Report saved", f.getAbsolutePath()));
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
        ctx.warehouse.controller().unsubscribe(plcListener);
        ctx.warehouse.removeOnChange(stockListener);
        views.forEach(View::dispose);
    }
}
