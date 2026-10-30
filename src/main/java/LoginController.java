import javafx.fxml.FXML;
import java.util.ArrayList;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML
    private TextField nameField;
    @FXML
    private TextField passField;
    @FXML
    private Label errorLabel;


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

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }
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