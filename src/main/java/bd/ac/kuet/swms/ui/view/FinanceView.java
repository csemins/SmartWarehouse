package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.service.FinanceService;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
    private FinanceService.Summary summary;

    @Override public String title() { return "Finance"; }

    @Override
    protected Node build() {
        HBox stats = new HBox(24, card("Income", income), card("Expenses", expense), card("Profit", profit), card("Stock value", stockValue));
        monthlyChart.setTitle("Income and expenses per month");
        monthlyChart.setAnimated(false);
        VBox overview = new VBox(12, stats, monthlyChart);
        TabPane inner = new TabPane(new Tab("Overview", scrolling(overview)), new Tab("Transactions", transactions.root()));
        inner.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        inner.getSelectionModel().selectedIndexProperty().addListener((o, old, index) -> refresh());
        return inner;
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
        });
    }
}
