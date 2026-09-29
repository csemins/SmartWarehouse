package bd.ac.kuet.swms.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Toasts {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObservableList<String> HISTORY = FXCollections.observableArrayList();
    private static final IntegerProperty UNREAD = new SimpleIntegerProperty();
    private static VBox host;

    private Toasts() { }

    public static void attach(VBox toastHost) { host = toastHost; }

    public static ObservableList<String> history() { return HISTORY; }

    public static IntegerProperty unread() { return UNREAD; }

    public static void show(String message) {
        HISTORY.add(0, TIME.format(LocalTime.now()) + "   " + message);
        if (HISTORY.size() > 30) HISTORY.remove(30);
        UNREAD.set(UNREAD.get() + 1);
        if (host == null) return;
        Label text = new Label(message);
        text.setWrapText(true);
        HBox toast = new HBox(12, Icons.picture("bell", 22), text);
        toast.getStyleClass().add("toast");
        host.getChildren().add(toast);
        if (host.getChildren().size() > 4) host.getChildren().remove(0);
        Motion.slideIn(toast, 420);
        FadeTransition out = new FadeTransition(Duration.millis(400), toast);
        out.setToValue(0);
        SequentialTransition life = new SequentialTransition(new PauseTransition(Duration.seconds(4.5)), out);
        life.setOnFinished(e -> host.getChildren().remove(toast));
        life.play();
    }
}
