import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Paths;
import java.util.ArrayList;


public class UsersWindow extends Application {
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

    public static void main(String[] args) {
        launch(args);
    }
}
