package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.ui.Icons;
import bd.ac.kuet.swms.ui.Navigator;
import bd.ac.kuet.swms.ui.TableExport;
import bd.ac.kuet.swms.ui.Toasts;
import bd.ac.kuet.swms.util.Formats;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ReportsView extends View {
    private final StatBox fulfilment = new StatBox("Order fulfilment", "trophy", "green");
    private final StatBox margin = new StatBox("Average catalogue margin", "chart-up", "indigo");
    private final StatBox potential = new StatBox("Stock at selling price", "money", "amber");
    private final StatBox shipped = new StatBox("Units shipped", "truck", "indigo");
    private final ComboBox<Integer> period = new ComboBox<>(FXCollections.observableArrayList(7, 30, 90, 365));
    private final VBox bestSellers = new VBox(12);
    private final PieChart orderStatus = new PieChart();
    private final TableView<ValuationLine> valuation = new TableView<>();
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);

    public record ValuationLine(String product, String category, int units, double costValue, double saleValue, double share) {
        double marginPercent() { return saleValue == 0 ? 0 : 100 * (saleValue - costValue) / saleValue; }
    }

    @Override public String title() { return "Reports"; }

    @Override
    protected Node build() {
        GridPane stats = new GridPane();
        stats.setHgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            stats.getColumnConstraints().add(column);
        }
        stats.addRow(0, fulfilment, margin, potential, shipped);

        period.setValue(30);
        period.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer days) { return days == null ? "" : "Last " + days + " days"; }
            @Override public Integer fromString(String text) { return period.getValue(); }
        });
        period.valueProperty().addListener((o, old, days) -> refresh());
        Button exportJson = new Button("Export JSON report", Icons.icon("file-json", 14));
        exportJson.setOnAction(e -> exportJsonReport());
        Button exportCsv = new Button("Export CSV", Icons.icon("download", 14));
        exportCsv.setOnAction(e -> TableExport.exportToCsv(valuation, "inventory-valuation.csv"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, period, spacer, exportCsv, exportJson);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        bestSellers.setPrefHeight(260);
        orderStatus.setLabelsVisible(false);
        orderStatus.setPrefHeight(260);
        GridPane charts = new GridPane();
        charts.setHgap(16);
        ColumnConstraints wide = new ColumnConstraints();
        wide.setPercentWidth(60);
        ColumnConstraints narrow = new ColumnConstraints();
        narrow.setPercentWidth(40);
        charts.getColumnConstraints().addAll(wide, narrow);
        charts.add(card("Best sellers", "Units shipped per product in the chosen period", bestSellers), 0, 0);
        charts.add(card("Orders by status", "All customer orders", orderStatus), 1, 0);

        addColumn(valuation, "Product", ValuationLine::product, 0.24);
        addColumn(valuation, "Category", ValuationLine::category, 0.14);
        addColumn(valuation, "Units", ValuationLine::units, 0.08);
        addColumn(valuation, "Cost value", v -> Formats.wholeMoney(v.costValue()), 0.13);
        addColumn(valuation, "Sale value", v -> Formats.wholeMoney(v.saleValue()), 0.13);
        addColumn(valuation, "Margin", v -> String.format("%.0f%%", v.marginPercent()), 0.08);
        TableColumn<ValuationLine, Double> share = addColumn(valuation, "Share of stock", ValuationLine::share, 0.2);
        share.setCellFactory(c -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();

            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setProgress(value == null ? 0 : value);
                setGraphic(empty || value == null ? null : bar);
            }
        });
        valuation.setPlaceholder(emptyState("bar-chart", "No stock yet", "Store boxes to see the inventory valuation."));
        valuation.setPrefHeight(320);

        VBox content = new VBox(16, toolbar, stats, charts, card("Inventory valuation", "Cost and selling value of the boxes in the rack", valuation));
        ctx.warehouse.onChange(changeListener);
        ScrollPane scroll = scrolling(content);
        return scroll;
    }

    @Override
    public void refresh() {
        int days = period.getValue() == null ? 30 : period.getValue();
        FxAsync.run(() -> new Object[]{ctx.products.findAll(), ctx.orders.countByStatus(), ctx.warehouse.shippedUnitsPerProduct(days, 8)}, data -> {
            @SuppressWarnings("unchecked") List<Product> products = (List<Product>) data[0];
            @SuppressWarnings("unchecked") Map<SalesOrder.Status, Integer> statuses = (Map<SalesOrder.Status, Integer>) data[1];
            @SuppressWarnings("unchecked") Map<String, Integer> best = (Map<String, Integer>) data[2];
            show(products, statuses, best);
        });
    }

    private void show(List<Product> products, Map<SalesOrder.Status, Integer> statuses, Map<String, Integer> best) {
        double totalCost = products.stream().mapToDouble(Product::getStockValue).sum();
        double totalSale = products.stream().mapToDouble(p -> p.getQuantity() * p.getUnitPrice()).sum();
        int done = statuses.get(SalesOrder.Status.DONE), all = statuses.values().stream().mapToInt(Integer::intValue).sum();
        fulfilment.countTo(all == 0 ? 0 : 100.0 * done / all, v -> String.format("%.0f%%", v));
        margin.countTo(products.stream().filter(p -> p.getUnitPrice() > 0)
                .mapToDouble(p -> 100 * (p.getUnitPrice() - p.getCostPrice()) / p.getUnitPrice()).average().orElse(0), v -> String.format("%.1f%%", v));
        potential.countTo(totalSale, Formats::wholeMoney);
        shipped.countTo(best.values().stream().mapToInt(Integer::intValue).sum(), v -> String.format("%,.0f", v));

        showBestSellers(best);
        orderStatus.setData(FXCollections.observableArrayList(statuses.entrySet().stream().filter(e -> e.getValue() > 0)
                .map(e -> new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue())).toList()));
        valuation.getItems().setAll(products.stream().filter(p -> p.getQuantity() > 0)
                .map(p -> new ValuationLine(p.getName(), p.getCategoryName() == null ? "" : p.getCategoryName(), p.getQuantity(),
                        p.getStockValue(), p.getQuantity() * p.getUnitPrice(), totalCost == 0 ? 0 : p.getStockValue() / totalCost))
                .sorted((a, b) -> Double.compare(b.costValue(), a.costValue())).toList());
    }

    private void showBestSellers(Map<String, Integer> best) {
        bestSellers.getChildren().clear();
        if (best.isEmpty()) {
            bestSellers.getChildren().add(emptyState("trophy", "No sales in this period", "Shipped boxes appear here."));
            return;
        }
        int max = best.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : best.entrySet()) {
            Label position = new Label(String.valueOf(rank++));
            position.getStyleClass().add("rank");
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("strong");
            Label units = hint(String.format("%,d units", entry.getValue()));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            ProgressBar bar = new ProgressBar(0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("thin-bar");
            VBox text = new VBox(4, new HBox(8, name, spacer, units), bar);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(12, position, text);
            row.setAlignment(Pos.CENTER_LEFT);
            bestSellers.getChildren().add(row);
            new Timeline(new KeyFrame(Duration.millis(700 + 60 * rank),
                    new KeyValue(bar.progressProperty(), (double) entry.getValue() / max, Interpolator.EASE_OUT))).play();
        }
    }

    private void exportJsonReport() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("inventory-report.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON file", "*.json"));
        File file = chooser.showSaveDialog(Navigator.get().stage());
        if (file != null) FxAsync.run(() -> { ctx.reports.exportInventoryReport(file.toPath()); return file; },
                f -> Toasts.show("Report saved to " + f.getName()));
    }

    @Override
    public void dispose() { ctx.warehouse.removeOnChange(changeListener); }
}
