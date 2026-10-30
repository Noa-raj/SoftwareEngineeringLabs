import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LoginControllerTest {
    private static ArrayList<User> users;

    @BeforeAll
    static void startJavaFxAndLoadTestUsers() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");

        URL fixture = LoginControllerTest.class.getResource("/users.txt");
        assertTrue(fixture != null, "Test users fixture was not found");
        ByteArrayOutputStream diagnosticsBytes = new ByteArrayOutputStream();
        try (PrintStream diagnostics =
                     new PrintStream(diagnosticsBytes, true, StandardCharsets.UTF_8)) {
            users = UsersApp.readUsers(Path.of(fixture.toURI()), diagnostics);
        }
        assertEquals(15, users.size(), "Invalid fixture row should be rejected");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void acceptsFixtureCredentialsAndRejectsIncorrectCredentials() throws Exception {
        for (User user : users) {
            LoginResult valid = attemptLogin(user.getUsername(), user.getPassword());
            assertFalse(valid.errorVisible(), "Expected valid credentials for "
                    + user.getUsername());
            assertEquals("Welcome", valid.windowTitle());
            assertEquals("Welcome", valid.welcomeText());
        }

        assertRejected("alice@example.com", "Wrong123!");
        assertRejected("unknown@example.com", "Alice123!");
        assertRejected("invalid-email", "NotAUser1!");
        assertRejected("alice@example.com", "alice123!");
        assertRejected("ALICE@example.com", "Alice123!");
        assertRejected("", "");
        assertRejected(" alice@example.com", "Alice123!");
        assertRejected("alice@example.com ", "Alice123!");
        assertRejected("alice@example.com", " Alice123!");
        assertRejected("alice@example.com", "Alice123! ");
    }

    private static void assertRejected(String username, String password) throws Exception {
        LoginResult result = attemptLogin(username, password);
        assertTrue(result.errorVisible(), "Expected rejected credentials: " + username);
        assertEquals("Software Engineering Labs", result.windowTitle());
        assertNull(result.welcomeText());
    }

    private static LoginResult attemptLogin(String username, String password) throws Exception {
        FutureTask<LoginResult> task = new FutureTask<>(() -> {
            URL layout = LoginControllerTest.class.getResource(
                    "/com/example/users/users-view.fxml");
            FXMLLoader loader = new FXMLLoader(layout);
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setUsers(users);

            TextField nameField = (TextField) root.lookup("#nameField");
            TextField passField = (TextField) root.lookup("#passField");
            Label errorLabel = (Label) root.lookup("#errorLabel");
            Button loginButton = (Button) root.lookup(".button");
            nameField.setText(username);
            passField.setText(password);

            Stage stage = new Stage();
            stage.setTitle("Software Engineering Labs");
            stage.setScene(new Scene(root));
            stage.show();
            try {
                loginButton.fire();
                String welcomeText = stage.getScene().getRoot()
                        .lookupAll(".label").stream()
                        .map(node -> ((Label) node).getText())
                        .filter("Welcome"::equals)
                        .findFirst()
                        .orElse(null);
                return new LoginResult(errorLabel.isVisible(), stage.getTitle(), welcomeText);
            } finally {
                stage.close();
            }
        });
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private record LoginResult(boolean errorVisible, String windowTitle, String welcomeText) {
    }
}
