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
        // hashing takes a moment, don't block the fx thread
        String user = username.getText();
        char[] pw = password.getText().toCharArray();
        FxAsync.run(() -> AppContext.get().auth.login(user, pw).isPresent(), ok -> {
            if (ok) Navigator.get().showMain();
            else {
                error.setText("Wrong username or password.");
                password.clear();
            }
        });
    }
}
