package bd.ac.kuet.swms;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SmartWarehouseApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // just checking javafx runs from maven, real screens come later
        stage.setTitle("Smart Warehouse Management System");
        stage.setScene(new Scene(new StackPane(new Label("Smart Warehouse - work in progress")), 900, 600));
        stage.show();
    }
}
