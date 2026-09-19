package bd.ac.kuet.swms.ui;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class UiThrottle<T> {
    private final AtomicReference<T> latest = new AtomicReference<>();
    private final AtomicBoolean updateScheduled = new AtomicBoolean();
    private final Consumer<T> updateOnFxThread;

    public UiThrottle(Consumer<T> updateOnFxThread) { this.updateOnFxThread = updateOnFxThread; }

    // plc publishes every 100ms, only schedule one runLater at a time, newest value wins
    public void submit(T value) {
        latest.set(value);
        if (updateScheduled.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                updateScheduled.set(false);
                updateOnFxThread.accept(latest.get());
            });
        }
    }
}
