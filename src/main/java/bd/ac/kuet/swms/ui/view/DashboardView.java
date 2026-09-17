package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.RackCell;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.service.FinanceService;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardView extends View {
    private final Label products = new Label();
    private final Label units = new Label();
    private final Label cells = new Label();
    private final Label openOrders = new Label();
    private final Label money = new Label();
    private final PieChart stockByCategory = new PieChart();
    private final BarChart<String, Number> unitsPerDay = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final ListView<String> restock = new ListView<>();
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);

    @Override public String title() { return "Dashboard"; }

    @Override
    protected Node build() {
        HBox stats = new HBox(16, card("Products", products), card("Units in stock", units), card("Rack cells used", cells), card("Open orders", openOrders));
        if (ctx.session.user().can(Permission.MANAGE_FINANCE)) stats.getChildren().add(card("Money", money));
        stockByCategory.setTitle("Stock value by category");
        unitsPerDay.setTitle("Units in / out (14 days)");
        restock.setPrefHeight(160);
        VBox content = new VBox(12, stats, new HBox(12, stockByCategory, unitsPerDay), card("Restock needed", restock));
        ctx.warehouse.onChange(changeListener);
        return scrolling(content);
    }

    @Override
    public void refresh() {
        // TODO this runs 6 queries one after another, make it parallel later
        FxAsync.run(() -> new Object[]{ctx.products.findAll(), ctx.warehouse.cells(), ctx.orders.openOrders().size(),
                ctx.finance.summary(), ctx.products.stockValueByCategory(), ctx.warehouse.unitsPerDay(14)}, data -> {
            @SuppressWarnings("unchecked") List<Product> list = (List<Product>) data[0];
            @SuppressWarnings("unchecked") List<RackCell> rack = (List<RackCell>) data[1];
            FinanceService.Summary summary = (FinanceService.Summary) data[3];
            @SuppressWarnings("unchecked") Map<String, Double> categories = (Map<String, Double>) data[4];
            @SuppressWarnings("unchecked") Map<LocalDate, int[]> days = (Map<LocalDate, int[]>) data[5];
            long used = rack.stream().filter(c -> c.getStatus() != RackCell.Status.EMPTY).count();
            products.setText(String.valueOf(list.size()));
            units.setText(String.valueOf(list.stream().mapToInt(Product::getQuantity).sum()));
            cells.setText(used + " / " + RackCell.COUNT);
            openOrders.setText(String.valueOf(data[2]));
            money.setText("Income " + Formats.money(summary.income()) + "\nExpenses " + Formats.money(summary.expense())
                    + "\nProfit " + Formats.money(summary.profit()));
            stockByCategory.setData(FXCollections.observableArrayList(categories.entrySet().stream()
                    .map(e -> new PieChart.Data(e.getKey(), e.getValue())).toList()));
            XYChart.Series<String, Number> in = new XYChart.Series<>();
            in.setName("In");
            XYChart.Series<String, Number> out = new XYChart.Series<>();
            out.setName("Out");
            days.forEach((day, u) -> {
                in.getData().add(new XYChart.Data<>(day.getDayOfMonth() + "/" + day.getMonthValue(), u[0]));
                out.getData().add(new XYChart.Data<>(day.getDayOfMonth() + "/" + day.getMonthValue(), u[1]));
            });
            unitsPerDay.getData().setAll(List.of(in, out));
            restock.getItems().setAll(list.stream().filter(Product::needsRestock)
                    .map(p -> p.getName() + "  (" + p.getQuantity() + " / " + p.getReorderLevel() + ")").toList());
        });
    }

    @Override
    public void dispose() { ctx.warehouse.removeOnChange(changeListener); }
}
