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
    private static AttemptTracker tracker;

    @BeforeAll
    static void startJavaFxAndLoadTestUsers() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");

        URL fixture = LoginControllerTest.class.getResource("/users.txt");
        assertTrue(fixture != null, "Test users fixture was not found");
        ByteArrayOutputStream diagnosticsBytes = new ByteArrayOutputStream();
        try (PrintStream diagnostics =
                     new PrintStream(diagnosticsBytes, true, StandardCharsets.UTF_8)) {
            users = UsersApp.readUsers(Path.of(fixture.toURI()), diagnostics);
        }
        assertEquals(15, users.size(), "Invalid fixture row should be rejected");
        tracker = new AttemptTracker(users, 5, 2);
        tracker.startWorkers();
    }

    @AfterAll
    static void stopJavaFx() {
        tracker.close();
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

        LoginResult wrongPassword = attemptLogin("alice@example.com", "Wrong123!");
        assertRejected(wrongPassword, "alice@example.com");
        assertTrue(wrongPassword.errorText().contains("Attempts remaining: 4"));

        LoginResult validAfterFailure = attemptLogin("alice@example.com", "Alice123!");
        assertFalse(validAfterFailure.errorVisible());
        LoginResult wrongAfterSuccess = attemptLogin("alice@example.com", "Wrong123!");
        assertRejected(wrongAfterSuccess, "alice@example.com");
        assertTrue(wrongAfterSuccess.errorText().contains("Attempts remaining: 4"));

        LoginResult unknownEmail = attemptLogin("unknown@example.com", "Alice123!");
        assertRejected(unknownEmail, "unknown@example.com");
        assertEquals("Invalid username or password", unknownEmail.errorText());

        assertRejected("invalid-email", "NotAUser1!");
        assertRejected("alice@example.com", "alice123!");
        assertRejected("ALICE@example.com", "Alice123!");
        assertRejected("", "");
        assertRejected(" alice@example.com", "Alice123!");
        assertRejected("alice@example.com ", "Alice123!");
        assertRejected("alice@example.com", " Alice123!");
        assertRejected("alice@example.com", "Alice123! ");

        UserState aliceState = tracker.getUserState("alice@example.com");
        aliceState.resetAfterSuccess();
        for (int attempt = 0; attempt < 5; attempt++) {
            tracker.recordFailure("alice@example.com");
        }
        LoginResult lockedWithCorrectPassword =
                attemptLogin("alice@example.com", "Alice123!");
        assertRejected(lockedWithCorrectPassword, "alice@example.com");
        assertTrue(lockedWithCorrectPassword.errorText().startsWith("Account locked."));
    }

    private static void assertRejected(String username, String password) throws Exception {
        assertRejected(attemptLogin(username, password), username);
    }

    private static void assertRejected(LoginResult result, String username) {
        assertTrue(result.errorVisible(), "Expected rejected credentials: " + username);
        assertEquals("Software Engineering Labs", result.windowTitle());
        assertNull(result.welcomeText());
    }

    private static LoginResult attemptLogin(String username, String password) throws Exception {
        FutureTask<Stage> task = new FutureTask<>(() -> {
            URL layout = LoginControllerTest.class.getResource(
                    "/com/example/users/users-view.fxml");
            FXMLLoader loader = new FXMLLoader(layout);
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setAttemptTracker(tracker);

            TextField nameField = (TextField) root.lookup("#nameField");
            TextField passField = (TextField) root.lookup("#passField");
            Button loginButton = (Button) root.lookup("#loginButton");
            nameField.setText(username);
            passField.setText(password);

            Stage stage = new Stage();
            stage.setTitle("Software Engineering Labs");
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest(event -> controller.shutdown());
            stage.show();
            loginButton.fire();
            return stage;
        });
        Platform.runLater(task);
        Stage stage = task.get(10, TimeUnit.SECONDS);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        LoginResult result = null;
        while (result == null && System.nanoTime() < deadline) {
            FutureTask<LoginResult> snapshot = new FutureTask<>(() -> {
                Button currentButton = (Button) stage.getScene().lookup("#loginButton");
                if (!"Welcome".equals(stage.getTitle()) && currentButton.isDisabled()) {
                    return null;
                }
                Label currentError = (Label) stage.getScene().lookup("#errorLabel");
                String welcomeText = stage.getScene().getRoot()
                        .lookupAll(".label").stream()
                        .map(node -> ((Label) node).getText())
                        .filter("Welcome"::equals)
                        .findFirst()
                        .orElse(null);
                return new LoginResult(currentError != null && currentError.isVisible(),
                        currentError == null ? "" : currentError.getText(),
                        stage.getTitle(), welcomeText);
            });
            Platform.runLater(snapshot);
            result = snapshot.get(10, TimeUnit.SECONDS);
            if (result == null) {
                Thread.sleep(20);
            }
        }
        FutureTask<Void> close = new FutureTask<>(() -> {
            stage.close();
            return null;
        });
        Platform.runLater(close);
        close.get(10, TimeUnit.SECONDS);
        if (result == null) {
            throw new AssertionError("Login attempt did not finish in time");
        }
        return result;
    }

    private record LoginResult(boolean errorVisible, String errorText, String windowTitle,
                               String welcomeText) {
    }
}
