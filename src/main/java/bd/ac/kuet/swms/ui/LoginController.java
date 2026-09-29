package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.AppContext;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LoginController {
    @FXML private StackPane root, logo;
    @FXML private VBox form;
    @FXML private Label title, error;
    @FXML private TextField username, passwordText;
    @FXML private PasswordField password;
    @FXML private CheckBox showPassword;
    @FXML private Button signIn;
    @FXML private ProgressIndicator busy;

    @FXML
    private void initialize() {
        // card never wider than 420, shrinks with the window
        form.maxWidthProperty().bind(Bindings.min(420, root.widthProperty().subtract(48)));
        logo.getChildren().add(Icons.logo(52));

        passwordText.textProperty().bindBidirectional(password.textProperty());
        passwordText.visibleProperty().bind(showPassword.selectedProperty());
        password.visibleProperty().bind(showPassword.selectedProperty().not());
        signIn.disableProperty().bind(username.textProperty().isEmpty().or(password.textProperty().isEmpty()).or(busy.visibleProperty()));
        error.managedProperty().bind(error.textProperty().isNotEmpty());

        Motion.enter(form, 80, 24);
    }

    @FXML
    private void onSignIn() {
        error.setText("");
        busy.setVisible(true);
        String user = username.getText();
        char[] pw = password.getText().toCharArray();
        FxAsync.run(() -> AppContext.get().auth.login(user, pw).isPresent(), ok -> {
            busy.setVisible(false);
            if (ok) Navigator.get().showMain();
            else {
                error.setText("Wrong username or password.");
                password.clear();
                shake();
            }
        }, e -> {
            busy.setVisible(false);
            error.setText(e.getMessage());
        });
    }

    private void shake() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), form);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}
