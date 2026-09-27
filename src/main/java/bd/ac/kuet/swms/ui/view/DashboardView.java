package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.RackCell;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.model.StockMovement;
import bd.ac.kuet.swms.service.FinanceService;
import bd.ac.kuet.swms.ui.Dialogs;
import bd.ac.kuet.swms.ui.Icons;
import bd.ac.kuet.swms.util.AppExecutors;
import bd.ac.kuet.swms.util.Formats;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DashboardView extends View {
    private final StatBox productCount = new StatBox("Products", "package", "indigo");
    private final StatBox unitsInStock = new StatBox("Units in stock", "cards", "green");
    private final StatBox usedCells = new StatBox("Rack cells used", "factory", "amber");
    private final StatBox openOrders = new StatBox("Open orders", "cart", "indigo");
    private final StatBox stockValue = new StatBox("Stock value (cost)", "coin", "amber");
    private final StatBox income = new StatBox("Income", "chart-up", "green");
    private final StatBox expense = new StatBox("Expenses", "chart-down", "red");
    private final StatBox profit = new StatBox("Profit", "money", "green");
    private final ProgressBar rackFill = new ProgressBar(0);
    private final Label rackFillText = new Label();
    private final PieChart salesByCategory = new PieChart();
    private final BarChart<String, Number> unitsPerDay = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final ListView<StockMovement> activity = new ListView<>();
    private final ListView<Product> restockList = new ListView<>();
    private final Label greeting = new Label();
    private final Label summary = new Label();
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);

    @Override public String title() { return "Dashboard"; }

    @Override
    protected Node build() {
        boolean showFinance = ctx.session.user().can(Permission.MANAGE_FINANCE);
        List<StatBox> boxes = showFinance
                ? List.of(productCount, unitsInStock, usedCells, openOrders, stockValue, income, expense, profit)
                : List.of(productCount, unitsInStock, usedCells, openOrders);
        GridPane stats = columns(4);
        for (int i = 0; i < boxes.size(); i++) stats.add(boxes.get(i), i % 4, i / 4);

        rackFill.setMaxWidth(Double.MAX_VALUE);
        rackFillText.getStyleClass().add("hint");
        VBox capacity = card("Rack capacity", rackFill, rackFillText);

        salesByCategory.setLegendVisible(true);
        salesByCategory.setLabelsVisible(false);
        unitsPerDay.setAnimated(true);
        unitsPerDay.setCategoryGap(8);
        unitsPerDay.setBarGap(2);
        GridPane charts = columns(2);
        charts.add(card("Sales by category", "Value of the boxes shipped in the last 30 days", salesByCategory), 0, 0);
        charts.add(card("Boxes in and out", "Units stored and shipped over the last 14 days", unitsPerDay), 1, 0);

        activity.setPlaceholder(emptyState("truck", "No boxes moved yet", "Stored and shipped boxes appear here."));
        activity.setCellFactory(list -> new ActivityCell());
        restockList.setPlaceholder(emptyState("check", "All good", "Every product is above its reorder level."));
        restockList.setCellFactory(list -> new RestockCell());
        GridPane lists = columns(2);
        lists.add(card("Recent activity", activity), 0, 0);
        lists.add(card("Restock needed", restockList), 1, 0);
        activity.setPrefHeight(260);
        restockList.setPrefHeight(260);

        greeting.getStyleClass().add("hero-title");
        summary.getStyleClass().add("hero-text");
        Label date = new Label(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").format(LocalDate.now()));
        date.getStyleClass().add("hero-text");
        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);
        HBox hero = new HBox(16, new VBox(6, date, greeting, summary), heroSpacer, Icons.picture("package", 84), Icons.picture("truck", 84));
        hero.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        hero.getStyleClass().addAll("card", "hero");
        VBox content = new VBox(16, hero, stats, capacity, charts, lists);
        salesByCategory.setPrefHeight(280);
        unitsPerDay.setPrefHeight(280);
        ctx.warehouse.onChange(changeListener);
        ScrollPane scroll = scrolling(content);
        return scroll;
    }

    private static GridPane columns(int count) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < count; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / count);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    @Override
    public void refresh() {
        // all queries in parallel, then one update on the fx thread
        var pool = AppExecutors.workers();
        var products = CompletableFuture.supplyAsync(ctx.products::findAll, pool);
        var categories = CompletableFuture.supplyAsync(() -> ctx.warehouse.salesPerCategory(30), pool);
        var days = CompletableFuture.supplyAsync(() -> ctx.warehouse.unitsPerDay(14), pool);
        var cells = CompletableFuture.supplyAsync(ctx.warehouse::cells, pool);
        var orders = CompletableFuture.supplyAsync(ctx.orders::openOrders, pool);
        var money = CompletableFuture.supplyAsync(ctx.finance::summary, pool);
        var latest = CompletableFuture.supplyAsync(() -> ctx.warehouse.latestMovements(20), pool);
        CompletableFuture.allOf(products, categories, days, cells, orders, money, latest).thenRun(() -> Platform.runLater(
                () -> show(products.join(), categories.join(), days.join(), cells.join(), orders.join(), money.join(), latest.join())))
                .exceptionally(error -> {
                    Platform.runLater(() -> Dialogs.error(error));
                    return null;
                });
    }

    private void show(List<Product> products, Map<String, Double> categories, Map<LocalDate, int[]> days, List<RackCell> cells,
                      List<SalesOrder> orders, FinanceService.Summary money, List<StockMovement> latest) {
        long used = cells.stream().filter(c -> c.getStatus() != RackCell.Status.EMPTY).count();
        productCount.countTo(products.size(), v -> String.format("%.0f", v));
        unitsInStock.countTo(products.stream().mapToInt(Product::getQuantity).sum(), v -> String.format("%,.0f", v));
        usedCells.countTo(used, v -> String.format("%.0f / %d", v, RackCell.COUNT));
        openOrders.countTo(orders.size(), v -> String.format("%.0f", v));
        stockValue.countTo(money.stockValue(), Formats::wholeMoney);
        income.countTo(money.income(), Formats::wholeMoney);
        expense.countTo(money.expense(), Formats::wholeMoney);
        profit.countTo(money.profit(), Formats::wholeMoney);
        profit.setTrend(money.income() == 0 ? "" : String.format("%.0f%% margin", 100 * money.profit() / money.income()), money.profit() >= 0);
        long restock = products.stream().filter(Product::needsRestock).count();
        greeting.setText("Welcome back, " + ctx.session.user().getUsername());
        summary.setText(orders.size() + " open order(s), " + restock + " product(s) to restock and "
                + (RackCell.COUNT - used) + " free rack cells.");
        new Timeline(new KeyFrame(Duration.millis(900), new KeyValue(rackFill.progressProperty(), (double) used / RackCell.COUNT, Interpolator.EASE_OUT))).play();
        rackFillText.setText(used + " of " + RackCell.COUNT + " cells hold a box, " + (RackCell.COUNT - used) + " are free");
        salesByCategory.setData(FXCollections.observableArrayList(categories.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey(), e.getValue())).toList()));
        XYChart.Series<String, Number> stored = new XYChart.Series<>();
        stored.setName("Stored");
        XYChart.Series<String, Number> shipped = new XYChart.Series<>();
        shipped.setName("Shipped");
        days.forEach((day, units) -> {
            String label = day.getDayOfMonth() + "/" + day.getMonthValue();
            stored.getData().add(new XYChart.Data<>(label, units[0]));
            shipped.getData().add(new XYChart.Data<>(label, units[1]));
        });
        unitsPerDay.getData().setAll(List.of(stored, shipped));
        activity.getItems().setAll(latest);
        restockList.getItems().setAll(products.stream().filter(Product::needsRestock).toList());
    }

    private static HBox row(Node icon, String color, String title, String detail, Node right) {
        StackPane badge = new StackPane(icon);
        badge.getStyleClass().addAll("stat-icon", color);
        badge.setStyle("-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; -fx-background-radius: 8;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 600;");
        VBox text = new VBox(1, titleLabel, hint(detail));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(12, badge, text, spacer, right);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

    private static class ActivityCell extends ListCell<StockMovement> {
        @Override
        protected void updateItem(StockMovement m, boolean empty) {
            super.updateItem(m, empty);
            setText(null);
            if (empty || m == null) { setGraphic(null); return; }
            boolean in = m.getType() == StockMovement.Type.IN;
            String detail = (in ? "Stored in cell " : "Shipped from cell ") + m.getCellId()
                    + (m.getOrderId() == null ? "" : " for order #" + m.getOrderId()) + " by " + m.getUsername();
            Label badge = new Label(in ? "IN" : "OUT");
            badge.getStyleClass().addAll("badge", in ? "badge-success" : "badge-warning");
            Label when = hint(Formats.timeAgo(m.getCreatedAt()));
            setGraphic(row(Icons.icon(in ? "box-in" : "box-out", 16), in ? "green" : "amber",
                    m.getQuantity() + " x " + m.getProductName(), detail, new VBox(4, badge, when)));
        }
    }

    private static class RestockCell extends ListCell<Product> {
        @Override
        protected void updateItem(Product p, boolean empty) {
            super.updateItem(p, empty);
            setText(null);
            if (empty || p == null) { setGraphic(null); return; }
            Label badge = new Label(p.getQuantity() + " / " + p.getReorderLevel());
            badge.getStyleClass().addAll("badge", "badge-danger");
            setGraphic(row(Icons.icon("alert", 16), "red", p.getName(),
                    "Only " + p.getQuantity() + " units left, reorder level is " + p.getReorderLevel(), badge));
        }
    }
}
