package bd.ac.kuet.swms.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class Navigator {
    private static Navigator instance;
    private final Stage stage;

    private Navigator(Stage stage) { this.stage = stage; }

    public static Navigator init(Stage stage) {
        instance = new Navigator(stage);
        return instance;
    }

    public static Navigator get() { return instance; }

    public Stage stage() { return stage; }

    public void showLogin() { show("Login.fxml", 900, 620); }

    public void showMain() { show("Main.fxml", 1280, 800); }

    // reuse one scene, just swap the root (no flicker between login and main)
    private void show(String fxml, double width, double height) {
        Parent root;
        try {
            root = FXMLLoader.load(Navigator.class.getResource("/bd/ac/kuet/swms/fxml/" + fxml));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, width, height));
        } else {
            stage.getScene().setRoot(root);
            if (!stage.isMaximized()) {
                stage.setWidth(width);
                stage.setHeight(height);
            }
        }
        Theme.apply(root);
        stage.getScene().getStylesheets().setAll(Navigator.class.getResource("/bd/ac/kuet/swms/css/app.css").toExternalForm());
        stage.centerOnScreen();
    }
}
