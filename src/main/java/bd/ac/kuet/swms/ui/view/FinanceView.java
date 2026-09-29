package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.net.ExchangeRateService;
import bd.ac.kuet.swms.service.FinanceService;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.AppExecutors;
import bd.ac.kuet.swms.util.Formats;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FinanceView extends View {
    private final TransactionsView transactions = new TransactionsView();
    private final StatBox income = new StatBox("Income", "chart-up", "green");
    private final StatBox expense = new StatBox("Expenses", "chart-down", "red");
    private final StatBox profit = new StatBox("Profit", "money", "green");
    private final StatBox stockValue = new StatBox("Stock value (cost)", "coin", "amber");
    private final BarChart<String, Number> monthlyChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final ComboBox<String> currency = new ComboBox<>(FXCollections.observableArrayList("USD", "EUR", "GBP", "INR", "CNY", "JPY"));
    private final Label rateInfo = new Label("Exchange rates are loaded from open.er-api.com");
    private final TableView<ConvertedValue> convertedValues = new TableView<>();
    private FinanceService.Summary summary;
    private ExchangeRateService.Rates rates;
    private ScheduledFuture<?> rateRefresh;

    private record ConvertedValue(String name, double taka, String converted) { }

    @Override public String title() { return "Finance"; }

    @Override
    protected Node build() {
        GridPane stats = new GridPane();
        stats.setHgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            stats.getColumnConstraints().add(column);
        }
        stats.addRow(0, income, expense, profit, stockValue);
        monthlyChart.setTitle("Income and expenses per month");
        monthlyChart.setAnimated(false);

        currency.setValue("USD");
        currency.setEditable(true);
        currency.valueProperty().addListener((o, old, value) -> showConvertedValues());
        Button reload = new Button("Reload rates");
        reload.setOnAction(e -> loadExchangeRates());
        addColumn(convertedValues, "", ConvertedValue::name, 0.34);
        addColumn(convertedValues, "BDT", v -> Formats.money(v.taka()), 0.33);
        addColumn(convertedValues, "Converted", ConvertedValue::converted, 0.33);
        convertedValues.setPrefHeight(150);
        convertedValues.setMinHeight(150);
        HBox currencyBar = new HBox(8, new Label("Currency"), currency, reload);
        currencyBar.setAlignment(Pos.CENTER_LEFT);

        monthlyChart.setTitle(null);
        rateInfo.getStyleClass().add("hint");
        VBox chartCard = card("Income and expenses per month", monthlyChart);
        VBox overview = new VBox(16, stats, chartCard, card("Values in another currency", "Live exchange rates fetched as JSON over HTTP", currencyBar, rateInfo, convertedValues));
        monthlyChart.setPrefHeight(260);
        overview.setPadding(new Insets(8, 0, 0, 0));

        TabPane inner = new TabPane(new Tab("Overview", scrolling(overview)), new Tab("Transactions", transactions.root()));
        inner.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        inner.getSelectionModel().selectedIndexProperty().addListener((o, old, index) -> refresh());
        // api updates once a day anyway, 30 min is plenty
        rateRefresh = AppExecutors.scheduler().scheduleAtFixedRate(() -> Platform.runLater(this::loadExchangeRates), 0, 30, TimeUnit.MINUTES);
        return inner;
    }

    private void loadExchangeRates() {
        FxAsync.run(() -> ctx.exchangeRates.fetch("BDT"), loaded -> {
            rates = loaded;
            rateInfo.setText("Rates for 1 BDT, updated " + loaded.updated() + " (refreshed automatically every 30 minutes)");
            showConvertedValues();
        }, e -> rateInfo.setText("Could not load exchange rates: " + e.getMessage()));
    }

    @Override
    public void refresh() {
        transactions.refresh();
        FxAsync.run(() -> new Object[]{ctx.finance.summary(), ctx.finance.totalsPerMonth(6)}, data -> {
            summary = (FinanceService.Summary) data[0];
            @SuppressWarnings("unchecked") Map<YearMonth, double[]> months = (Map<YearMonth, double[]>) data[1];
            income.countTo(summary.income(), Formats::wholeMoney);
            expense.countTo(summary.expense(), Formats::wholeMoney);
            profit.countTo(summary.profit(), Formats::wholeMoney);
            stockValue.countTo(summary.stockValue(), Formats::wholeMoney);
            XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
            incomeSeries.setName("Income");
            XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
            expenseSeries.setName("Expenses");
            months.forEach((month, totals) -> {
                incomeSeries.getData().add(new XYChart.Data<>(month.toString(), totals[0]));
                expenseSeries.getData().add(new XYChart.Data<>(month.toString(), totals[1]));
            });
            monthlyChart.getData().setAll(List.of(incomeSeries, expenseSeries));
            showConvertedValues();
        });
    }

    private void showConvertedValues() {
        if (summary == null) return;
        String code = currency.getValue() == null ? "USD" : currency.getValue().trim().toUpperCase();
        convertedValues.getItems().setAll(List.of(
                convert("Income", summary.income(), code),
                convert("Expenses", summary.expense(), code),
                convert("Profit", summary.profit(), code),
                convert("Stock value", summary.stockValue(), code)));
    }

    private ConvertedValue convert(String name, double taka, String code) {
        String converted = rates == null || !rates.rates().containsKey(code) ? "-" : Formats.money(rates.convert(taka, code), code);
        return new ConvertedValue(name, taka, converted);
    }

    @Override
    public void dispose() {
        if (rateRefresh != null) rateRefresh.cancel(false);
    }
}
