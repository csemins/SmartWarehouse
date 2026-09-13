package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.net.ExchangeRateService;
import bd.ac.kuet.swms.service.FinanceService;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class FinanceView extends View {
    private final TransactionsView transactions = new TransactionsView();
    private final Label income = new Label();
    private final Label expense = new Label();
    private final Label profit = new Label();
    private final Label stockValue = new Label();
    private final BarChart<String, Number> monthlyChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final ComboBox<String> currency = new ComboBox<>(FXCollections.observableArrayList("USD", "EUR", "GBP", "INR", "CNY", "JPY"));
    private final Label rateInfo = new Label("Exchange rates are loaded from open.er-api.com");
    private final Label converted = new Label();
    private ExchangeRateService.Rates rates;
    private FinanceService.Summary summary;

    @Override public String title() { return "Finance"; }

    @Override
    protected Node build() {
        HBox stats = new HBox(24, card("Income", income), card("Expenses", expense), card("Profit", profit), card("Stock value", stockValue));
        monthlyChart.setTitle("Income and expenses per month");
        monthlyChart.setAnimated(false);
        VBox overview = new VBox(12, stats, monthlyChart);
        currency.setValue("USD");
        currency.setEditable(true);
        currency.valueProperty().addListener((o, old, value) -> showConverted());
        Button reload = new Button("Reload rates");
        reload.setOnAction(e -> loadExchangeRates());
        overview.getChildren().add(card("Values in another currency", new HBox(8, new Label("Currency"), currency, reload), rateInfo, converted));
        loadExchangeRates();
        TabPane inner = new TabPane(new Tab("Overview", scrolling(overview)), new Tab("Transactions", transactions.root()));
        inner.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        inner.getSelectionModel().selectedIndexProperty().addListener((o, old, index) -> refresh());
        return inner;
    }

    private void loadExchangeRates() {
        FxAsync.run(() -> ctx.exchangeRates.fetch("BDT"), loaded -> {
            rates = loaded;
            rateInfo.setText("Rates for 1 BDT, updated " + loaded.updated());
            showConverted();
        }, e -> rateInfo.setText("Could not load exchange rates: " + e.getMessage()));
    }

    private void showConverted() {
        if (summary == null || rates == null) return;
        String code = currency.getValue() == null ? "USD" : currency.getValue().trim().toUpperCase();
        if (!rates.rates().containsKey(code)) { converted.setText("Unknown currency " + code); return; }
        converted.setText("Profit: " + Formats.money(rates.convert(summary.profit(), code), code)
                + "    Stock value: " + Formats.money(rates.convert(summary.stockValue(), code), code));
    }

    @Override
    public void refresh() {
        transactions.refresh();
        FxAsync.run(() -> new Object[]{ctx.finance.summary(), ctx.finance.totalsPerMonth(6)}, data -> {
            summary = (FinanceService.Summary) data[0];
            @SuppressWarnings("unchecked") Map<YearMonth, double[]> months = (Map<YearMonth, double[]>) data[1];
            income.setText(Formats.money(summary.income()));
            expense.setText(Formats.money(summary.expense()));
            profit.setText(Formats.money(summary.profit()));
            stockValue.setText(Formats.money(summary.stockValue()));
            XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
            incomeSeries.setName("Income");
            XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
            expenseSeries.setName("Expenses");
            months.forEach((month, totals) -> {
                incomeSeries.getData().add(new XYChart.Data<>(month.toString(), totals[0]));
                expenseSeries.getData().add(new XYChart.Data<>(month.toString(), totals[1]));
            });
            monthlyChart.getData().setAll(List.of(incomeSeries, expenseSeries));
            showConverted();
        });
    }
}
