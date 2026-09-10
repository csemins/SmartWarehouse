package bd.ac.kuet.swms;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.ui.Navigator;
import bd.ac.kuet.swms.util.AppExecutors;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;

public class SmartWarehouseApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        String folder = getParameters().getNamed().get("data");
        AppContext context = AppContext.start(folder == null ? Database.dataFolder() : Path.of(folder));
        context.automation.start();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Smart Warehouse Management System");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        Navigator.init(stage).showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        AppContext.get().automation.stop();
        AppContext.get().warehouse.disconnect();
        AppExecutors.shutdown();
    }
}
