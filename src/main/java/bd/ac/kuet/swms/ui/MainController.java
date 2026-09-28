package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.AppContext;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.ui.view.ActivityView;
import bd.ac.kuet.swms.ui.view.DashboardView;
import bd.ac.kuet.swms.ui.view.FinanceView;
import bd.ac.kuet.swms.ui.view.MovementsView;
import bd.ac.kuet.swms.ui.view.OrdersView;
import bd.ac.kuet.swms.ui.view.ProductsView;
import bd.ac.kuet.swms.ui.view.ReportsView;
import bd.ac.kuet.swms.ui.view.SettingsView;
import bd.ac.kuet.swms.ui.view.TableFormView;
import bd.ac.kuet.swms.ui.view.View;
import bd.ac.kuet.swms.ui.view.WarehouseView;
import bd.ac.kuet.swms.util.AppExecutors;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainController {
    @FXML private BorderPane root;
    @FXML private VBox nav, toastHost;
    @FXML private StackPane pageHost, brandLogo;
    @FXML private Region navIndicator;
    @FXML private Label restockAlert, userName, userRole, avatar, clock, jobStatus, pageTitle, pageSubtitle, bellBadge;
    @FXML private Button logoutButton, themeButton, bellButton, searchButton;
    @FXML private MenuButton moreMenu;

    private final AppContext ctx = AppContext.get();
    private final Map<ToggleButton, Page> pages = new LinkedHashMap<>();
    private final ToggleGroup navGroup = new ToggleGroup();
    private final Consumer<Snapshot> plcListener = new UiThrottle<Snapshot>(this::showCraneJob)::submit;
    private final Runnable stockListener = () -> Platform.runLater(this::refreshRestockAlert);
    private final Consumer<String> messageListener = message -> Platform.runLater(() -> Toasts.show(message));
    private ScheduledFuture<?> clockTask;
    private CommandPalette palette;

    private record Module(Permission permission, String icon, String subtitle, Supplier<View> create) { }

    private record Page(View view, String icon, String subtitle) { }

    // order here = order in the sidebar = ctrl+1..9
    private static final List<Module> MODULES = List.of(
            new Module(Permission.VIEW_DASHBOARD, "dashboard", "Overview of stock, orders and money", DashboardView::new),
            new Module(Permission.OPERATE_WAREHOUSE, "warehouse", "Rack cells and Factory IO crane jobs", WarehouseView::new),
            new Module(Permission.MANAGE_ORDERS, "orders", "Customer orders shipped automatically", OrdersView::new),
            new Module(Permission.MANAGE_PRODUCTS, "products", "Catalogue, prices, photos and putting boxes into stock", ProductsView::new),
            new Module(Permission.VIEW_MOVEMENTS, "movements", "Every box that went in or out", MovementsView::new),
            new Module(Permission.MANAGE_FINANCE, "finance", "Income, expenses and live exchange rates", FinanceView::new),
            new Module(Permission.VIEW_DASHBOARD, "chart-column", "Best sellers, fulfilment and inventory valuation", ReportsView::new),
            new Module(Permission.CONFIGURE_SYSTEM, "history", "Security and change history of every user", ActivityView::new),
            new Module(Permission.CONFIGURE_SYSTEM, "settings", "Factory IO connection and manual control", SettingsView::new));

    @FXML
    private void initialize() {
        User user = ctx.session.user();
        userName.setText(user.getFullName());
        userRole.setText(user.getRole().toString());
        avatar.setText(initials(user.getFullName()));
        brandLogo.getChildren().add(Icons.logo(36));
        logoutButton.setGraphic(Icons.icon("logout", 16));
        moreMenu.setGraphic(Icons.icon("more", 18));
        bellButton.setGraphic(Icons.icon("bell", 18));
        searchButton.setGraphic(Icons.icon("search", 14));
        bellBadge.textProperty().bind(Toasts.unread().asString());
        bellBadge.visibleProperty().bind(Toasts.unread().greaterThan(0));
        updateThemeIcon();

        for (Module module : MODULES) {
            if (!user.can(module.permission())) continue;
            View view = module.create().get();
            ToggleButton button = new ToggleButton(view.title(), Icons.icon(module.icon(), 18));
            button.setId("nav-" + view.title().replace(' ', '-'));
            button.getStyleClass().setAll("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setToggleGroup(navGroup);
            button.setOnAction(e -> { if (!button.isSelected()) button.setSelected(true); });
            button.selectedProperty().addListener((o, was, now) -> { if (now) show(button); });
            pages.put(button, new Page(view, module.icon(), module.subtitle()));
            nav.getChildren().add(button);
        }
        navIndicator.resize(4, 22);
        ((ToggleButton) nav.getChildren().get(0)).setSelected(true);

        Toasts.attach(toastHost);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy   HH:mm:ss");
        clockTask = AppExecutors.scheduler().scheduleAtFixedRate(
                () -> Platform.runLater(() -> clock.setText(format.format(LocalDateTime.now()))), 0, 1, TimeUnit.SECONDS);
        ctx.warehouse.controller().subscribe(plcListener);
        ctx.warehouse.onChange(stockListener);
        ctx.warehouse.onMessage(messageListener);
        root.sceneProperty().addListener((o, old, scene) -> { if (scene != null) installShortcuts(scene); });
        refreshRestockAlert();
    }

    private void show(ToggleButton button) {
        Page page = pages.get(button);
        pageTitle.setText(page.view().title());
        pageSubtitle.setText(page.subtitle());
        pageHost.getChildren().setAll(page.view().root());
        Motion.enter(page.view().root(), 0, 10);
        Motion.staggerCards(page.view().root());
        page.view().refresh();
        Platform.runLater(() -> moveIndicator(button));
    }

    // runLater because the button has no bounds yet on first show
    private void moveIndicator(ToggleButton button) {
        double y = button.getBoundsInParent().getMinY() + (button.getHeight() - 22) / 2;
        new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(navIndicator.layoutYProperty(), y, Interpolator.SPLINE(0.2, 0.8, 0.2, 1)))).play();
    }

    private void installShortcuts(Scene scene) {
        List<ToggleButton> buttons = new ArrayList<>(pages.keySet());
        KeyCode[] digits = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5,
                KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9};
        for (int i = 0; i < buttons.size() && i < digits.length; i++) {
            ToggleButton button = buttons.get(i);
            scene.getAccelerators().put(new KeyCodeCombination(digits[i], KeyCombination.SHORTCUT_DOWN), () -> button.setSelected(true));
        }
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), this::onSearch);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), this::onToggleTheme);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11),
                () -> Navigator.get().stage().setFullScreen(!Navigator.get().stage().isFullScreen()));
    }

    private static String initials(String name) {
        return Arrays.stream(name.trim().split("\\s+")).limit(2).map(w -> w.substring(0, 1).toUpperCase()).collect(Collectors.joining());
    }

    private void showCraneJob(Snapshot s) {
        jobStatus.setText(s.currentJob() == null ? "Crane idle" : "Crane: " + s.currentJob() + "  -  " + s.currentJob().progress());
    }

    private void refreshRestockAlert() {
        FxAsync.run(() -> ctx.products.productsToRestock().size(),
                count -> {
                    restockAlert.setText(count + " product" + (count == 1 ? "" : "s") + " to restock");
                    restockAlert.setVisible(count > 0);
                    restockAlert.setManaged(count > 0);
                });
    }

    @FXML
    private void onSearch() {
        if (palette == null) palette = new CommandPalette(Navigator.get().stage(), this::searchEntries);
        if (!palette.isShowing()) palette.show();
    }

    private List<CommandPalette.Entry> searchEntries() {
        List<CommandPalette.Entry> entries = new ArrayList<>();
        pages.forEach((button, page) -> entries.add(new CommandPalette.Entry(page.view().title(), page.subtitle(), page.icon(),
                () -> button.setSelected(true))));
        ToggleButton productsButton = buttonFor(ProductsView.class);
        if (productsButton != null) for (Product p : ctx.products.findAll())
            entries.add(new CommandPalette.Entry(p.getName(), "Product  ·  " + p.getSku() + "  ·  " + p.getQuantity() + " units", "products",
                    () -> openAndSearch(productsButton, p.getSku())));
        ToggleButton ordersButton = buttonFor(OrdersView.class);
        if (ordersButton != null) for (SalesOrder o : ctx.orders.findAll())
            entries.add(new CommandPalette.Entry("Order #" + o.getId() + "  " + o.getCustomer(), "Order  ·  " + o.getProductName() + "  ·  " + o.getStatus(), "orders",
                    () -> openAndSearch(ordersButton, o.getCustomer())));
        return entries;
    }

    private ToggleButton buttonFor(Class<? extends View> type) {
        return pages.entrySet().stream().filter(e -> type.isInstance(e.getValue().view())).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private void openAndSearch(ToggleButton button, String text) {
        button.setSelected(true);
        if (pages.get(button).view() instanceof TableFormView<?> table) table.search(text);
    }

    @FXML
    private void onNotifications() {
        ContextMenu menu = new ContextMenu();
        if (Toasts.history().isEmpty()) {
            MenuItem none = new MenuItem("No notifications yet");
            none.setDisable(true);
            menu.getItems().add(none);
        }
        Toasts.history().stream().limit(12).forEach(text -> menu.getItems().add(new MenuItem(text)));
        if (!Toasts.history().isEmpty()) {
            MenuItem clear = new MenuItem("Clear all");
            clear.setOnAction(e -> Toasts.history().clear());
            menu.getItems().addAll(new SeparatorMenuItem(), clear);
        }
        Toasts.unread().set(0);
        menu.show(bellButton, Side.BOTTOM, -260, 6);
    }

    @FXML
    private void onToggleTheme() {
        Theme.setDark(!Theme.isDark());
        Theme.apply(root.getScene().getRoot());
        updateThemeIcon();
        AppExecutors.workers().submit(() -> ctx.warehouse.saveDarkMode(Theme.isDark()));
    }

    private void updateThemeIcon() { themeButton.setGraphic(Icons.icon(Theme.isDark() ? "sun" : "moon", 18)); }

    @FXML
    private void onExportReport() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("inventory-report.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON file", "*.json"));
        File file = chooser.showSaveDialog(Navigator.get().stage());
        if (file != null) FxAsync.run(() -> { ctx.reports.exportInventoryReport(file.toPath()); return file; },
                f -> Toasts.show("Report saved to " + f.getName()));
    }

    @FXML
    private void onShortcuts() {
        Dialogs.info("Keyboard shortcuts", """
                Ctrl+K       Search pages, products and orders
                Ctrl+1..9    Open the pages in the sidebar
                Ctrl+D       Light / dark theme
                F11          Full screen""");
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

    @FXML
    private void onAbout() {
        Dialogs.info("Smart Warehouse Management System",
                "Inventory, orders, finance, reports and Factory IO control in one application.\nJava "
                        + System.getProperty("java.version") + ", JavaFX " + System.getProperty("javafx.version"));
    }

    private void close() {
        clockTask.cancel(false);
        root.getScene().getAccelerators().clear();
        ctx.warehouse.controller().unsubscribe(plcListener);
        ctx.warehouse.removeOnChange(stockListener);
        ctx.warehouse.removeOnMessage(messageListener);
        Toasts.attach(null);
        pages.values().forEach(page -> page.view().dispose());
    }
}
