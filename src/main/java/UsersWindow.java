import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Paths;
import java.util.ArrayList;


/**
 * Starts the JavaFX application, reads available users, and displays the login screen.
 */
public class UsersWindow extends Application {
    /**
     * Loads the login layout, provides its controller with the users read from {@code users.txt},
     * and shows the primary stage.
     *
     * @param stage primary application window
     * @throws IOException if the user file or FXML cannot be read
     */
    @Override
    public void start(Stage stage) throws IOException {
        ArrayList<User> users =
                UsersApp.readUsers(Paths.get("users.txt"), System.err);
        URL layout = Objects.requireNonNull(
                UsersWindow.class.getResource("/com/example/users/users-view.fxml"),
                "Missing users-view.fxml on the classpath");
        FXMLLoader loader = new FXMLLoader(layout);
        Scene scene = new Scene(loader.load());

        LoginController controller = loader.getController();
        controller.setUsers(users);

        stage.setScene(scene);
        stage.setTitle("Software Engineering Labs");
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments passed to JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }
}
