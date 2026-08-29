package bd.ac.kuet.swms.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class Dialogs {
    private Dialogs() { }

    public static void info(String header, String text) { show(Alert.AlertType.INFORMATION, header, text); }

    public static void error(String header, String text) { show(Alert.AlertType.ERROR, header, text); }

    public static void error(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && (cause.getMessage() == null || cause.getClass() == RuntimeException.class)) cause = cause.getCause();
        error("Something went wrong", cause.getMessage() != null ? cause.getMessage() : cause.toString());
    }

    public static boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.initOwner(Navigator.get().stage());
        alert.setHeaderText(null);
        return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    private static void show(Alert.AlertType type, String header, String text) {
        Alert alert = new Alert(type, text, ButtonType.OK);
        alert.initOwner(Navigator.get().stage());
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
