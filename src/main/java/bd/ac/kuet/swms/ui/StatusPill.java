package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.plc.ControllerState;
import javafx.scene.control.Label;

public final class StatusPill {
    private StatusPill() { }

    public static String styleFor(ControllerState state) {
        return switch (state) {
            case RUNNING -> "badge-success";
            case IDLE, PAUSED -> "badge-warning";
            case FAULT -> "badge-danger";
            case DISCONNECTED -> "badge-neutral";
        };
    }

    public static void show(Label label, ControllerState state) {
        label.setText(state.name());
        label.getStyleClass().setAll("label", "badge", styleFor(state));
    }
}
