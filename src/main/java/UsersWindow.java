import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Starts the JavaFX application, reads users away from the UI thread, and displays the login form.
 */
public class UsersWindow extends Application {
    /**
     * Shows a loading screen and reads the user file on a background thread before creating the
     * login screen on the JavaFX application thread.
     *
     * @param stage primary application window
     */
    @Override
    public void start(Stage stage) {
        int maximumAttempts = Integer.parseInt(getParameters().getRaw().get(0));
        int lockoutSeconds = Integer.parseInt(getParameters().getRaw().get(1));
        AtomicBoolean closing = new AtomicBoolean();
        AtomicReference<AttemptTracker> trackerReference = new AtomicReference<>();
        AtomicReference<LoginController> controllerReference = new AtomicReference<>();
        AtomicReference<Thread> fileLoaderReference = new AtomicReference<>();

        stage.setTitle("Software Engineering Labs");
        stage.setScene(new Scene(new Label("Loading users..."), 400, 200));
        stage.setOnCloseRequest(event -> {
            closing.set(true);
            Thread fileLoader = fileLoaderReference.get();
            if (fileLoader != null) {
                fileLoader.interrupt();
            }
            LoginController controller = controllerReference.get();
            if (controller != null) {
                controller.shutdown();
            }
            AttemptTracker tracker = trackerReference.get();
            if (tracker != null) {
                tracker.close();
            }
        });
        stage.show();

        Thread fileLoader = new Thread(() -> {
            try {
                ArrayList<User> users = UsersApp.readUsers(Paths.get("users.txt"), System.err);
                if (Thread.currentThread().isInterrupted() || closing.get()) {
                    return;
                }
                Platform.runLater(() -> showLogin(
                        stage, users, maximumAttempts, lockoutSeconds,
                        trackerReference, controllerReference));
            } catch (IOException exception) {
                if (Thread.currentThread().isInterrupted() || closing.get()) {
                    return;
                }
                System.err.println("Could not read users.txt: " + exception.getMessage());
                Platform.runLater(() -> showStartupError(stage, exception));
            }
        }, "users-file-loader");
        fileLoader.setDaemon(true);
        fileLoaderReference.set(fileLoader);
        fileLoader.start();
    }

    /**
     * Validates n and t before launching JavaFX. Missing or invalid values are requested again
     * through the console.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        String[] suppliedArguments = args.length > 2 ? new String[0] : args;
        if (args.length > 2) {
            System.out.println("Enter exactly two positive integers: n and t.");
        }
        int maximumAttempts = readPositiveArgument(suppliedArguments, 0, "n", input);
        int lockoutSeconds = readPositiveArgument(suppliedArguments, 1, "t", input);
        launch(UsersWindow.class,
                new String[]{Integer.toString(maximumAttempts), Integer.toString(lockoutSeconds)});
    }

    static int readPositiveArgument(String[] args, int index, String name, Scanner input) {
        String value = index < args.length ? args[index] : null;
        while (true) {
            if (value == null) {
                System.out.print("Enter a positive integer for " + name + ": ");
                System.out.flush();
                if (!input.hasNextLine()) {
                    throw new IllegalStateException("No console input available for " + name);
                }
                value = input.nextLine();
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed >= 1) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input is reported below, then requested again.
            }
            System.out.println(name + " must be a positive integer.");
            value = null;
        }
    }

    private static void showLogin(Stage stage, ArrayList<User> users, int maximumAttempts,
                                  int lockoutSeconds,
                                  AtomicReference<AttemptTracker> trackerReference,
                                  AtomicReference<LoginController> controllerReference) {
        AttemptTracker tracker = new AttemptTracker(users, maximumAttempts, lockoutSeconds);
        tracker.startWorkers();
        try {
            URL layout = Objects.requireNonNull(
                    UsersWindow.class.getResource("/com/example/users/users-view.fxml"),
                    "Missing users-view.fxml on the classpath");
            FXMLLoader loader = new FXMLLoader(layout);
            Scene scene = new Scene(loader.load());
            LoginController controller = loader.getController();
            controller.setAttemptTracker(tracker);
            trackerReference.set(tracker);
            controllerReference.set(controller);
            stage.setScene(scene);
            stage.setTitle("Software Engineering Labs");
            stage.show();
        } catch (IOException exception) {
            tracker.close();
            showStartupError(stage, exception);
        }
    }

    private static void showStartupError(Stage stage, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Unable to start");
        alert.setHeaderText("The login screen could not be loaded.");
        alert.setContentText(exception.getMessage());
        alert.show();
    }
}
