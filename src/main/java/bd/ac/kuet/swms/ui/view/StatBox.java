package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.ui.Icons;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.DoubleFunction;

public class StatBox extends HBox {
    private final Label value = new Label("-");
    private final Label trend = new Label();
    private final DoubleProperty shown = new SimpleDoubleProperty();
    private DoubleFunction<String> format = v -> String.format("%,.0f", v);
    private Timeline counter;

    public StatBox(String caption, String picture, String color) {
        StackPane badge = new StackPane(Icons.picture(picture, 30));
        badge.getStyleClass().addAll("stat-icon", color);
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");
        value.getStyleClass().add("stat-value");
        trend.getStyleClass().add("stat-trend");
        trend.setVisible(false);
        trend.setManaged(false);
        getChildren().addAll(badge, new VBox(2, captionLabel, value, trend));
        getStyleClass().addAll("card", "stat-card");
        setMaxWidth(Double.MAX_VALUE);
        shown.addListener((o, old, v) -> value.setText(format.apply(v.doubleValue())));
    }

    public void setValue(String text) {
        if (counter != null) counter.stop();
        value.setText(text);
    }

    public void countTo(double target, DoubleFunction<String> formatter) {
        format = formatter;
        if (counter != null) counter.stop();
        value.setText(format.apply(shown.get()));
        counter = new Timeline(new KeyFrame(Duration.millis(900), new KeyValue(shown, target, javafx.animation.Interpolator.EASE_OUT)));
        counter.play();
    }

    public void setTrend(String text, boolean good) {
        trend.setText(text);
        trend.getStyleClass().removeAll("good", "bad");
        trend.getStyleClass().add(good ? "good" : "bad");
        trend.setVisible(!text.isEmpty());
        trend.setManaged(!text.isEmpty());
    }
}
