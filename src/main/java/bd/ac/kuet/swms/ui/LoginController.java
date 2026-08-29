package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML private VBox form;
    @FXML private Label error;
    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Button signIn;

    @FXML
    private void initialize() {
        signIn.disableProperty().bind(username.textProperty().isEmpty().or(password.textProperty().isEmpty()));
    }

    @FXML
    private void onSignIn() {
        error.setText("");
        if (AppContext.get().auth.login(username.getText(), password.getText().toCharArray()).isPresent()) {
            Navigator.get().showMain();
        } else {
            error.setText("Wrong username or password.");
            password.clear();
        }
    }
}
