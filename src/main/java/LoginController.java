import javafx.fxml.FXML;
import java.util.ArrayList;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Handles login form actions and switches to the welcome screen for matching credentials.
 */
public class LoginController {

    /** Username input field injected from the login FXML. */
    @FXML
    private TextField nameField;
    /** Password input field injected from the login FXML. */
    @FXML
    private TextField passField;
    /** Error message shown when the entered credentials do not match a user. */
    @FXML
    private Label errorLabel;

    /**
     * Checks the entered credentials against the users supplied by the application window.
     * A match opens the welcome screen; otherwise, the error label is shown.
     */
    @FXML
    private void click() {
        String username = nameField.getText();
        String password = passField.getText();

        boolean found = false;

        for (User user : users) {
            if (user.getUsername().equals(username) &&
                    user.getPassword().equals(password)) {

                found = true;
                break;
            }
        }

        if (found) {
            errorLabel.setVisible(false);
            showWelcome();
        } else {
            errorLabel.setVisible(true);
        }
    }

    private ArrayList<User> users;

    /**
     * Supplies the users that can log in through this screen.
     *
     * @param users valid users read from the application input file
     */
    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    /** Loads the welcome FXML and displays it in the current window. */
    private void showWelcome() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/com/example/users/welcome-view.fxml"
                    )
            );

            Stage stage = (Stage) nameField
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Welcome");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}