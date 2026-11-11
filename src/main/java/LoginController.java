import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Starts background login and countdown tasks and applies their results to the login screen.
 */
public class LoginController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField passField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button concurrencyDemoButton;
    @FXML
    private Label concurrencyDemoLabel;

    private final AtomicReference<Thread> loginThread = new AtomicReference<>();
    private final AtomicReference<Thread> countdownThread = new AtomicReference<>();
    private final AtomicReference<Thread> demoThread = new AtomicReference<>();
    private AttemptTracker attemptTracker;

    /**
     * Captures the form values and processes the attempt on a new worker thread.
     */
    @FXML
    private void click() {
        String email = nameField.getText();
        String password = passField.getText();
        stopCountdown();
        setControlsDisabled(true);
        errorLabel.setVisible(false);

        Thread taskThread = new Thread(
                new LoginTask(email, password, attemptTracker, result -> showResult(email, result)),
                "login-task");
        loginThread.set(taskThread);
        taskThread.start();
    }

    /** Runs the lockout threshold and synchronized-counter demonstrations off the UI thread. */
    @FXML
    private void runConcurrencyDemo() {
        concurrencyDemoButton.setDisable(true);
        concurrencyDemoLabel.setText("Running concurrency demonstration...");
        Thread taskThread = new Thread(() -> {
            try {
                String email = nameField.getText();
                ConcurrencyDemo.LoginThresholdResult loginResult =
                        ConcurrencyDemo.runLoginThreshold(attemptTracker, email);
                ConcurrencyDemo.Result result = ConcurrencyDemo.run(20, 100);
                javafx.application.Platform.runLater(() -> {
                    concurrencyDemoLabel.setText(
                            email + ": " + loginResult.observedFailures()
                                    + " concurrent failures counted; not locked at "
                                    + loginResult.attemptsBeforeThreshold() + ": "
                                    + loginResult.notLockedBeforeThreshold()
                                    + "; locked at n=" + loginResult.threshold() + ": "
                                    + loginResult.lockedAtThreshold()
                                    + ". Counter demo (expected " + result.expectedCount()
                                    + "): without synchronized "
                                    + result.unsynchronizedCount()
                                    + ", with synchronized " + result.synchronizedCount());
                    concurrencyDemoButton.setDisable(false);
                    demoThread.set(null);
                });
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                javafx.application.Platform.runLater(() -> {
                    concurrencyDemoLabel.setText(
                            "Concurrency demonstration failed: " + exception.getMessage());
                    concurrencyDemoButton.setDisable(false);
                    demoThread.set(null);
                });
            }
        }, "concurrency-demonstration");
        demoThread.set(taskThread);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    /** Supplies the shared login state and its failure workers. */
    public void setAttemptTracker(AttemptTracker attemptTracker) {
        this.attemptTracker = attemptTracker;
    }

    /** Stops login and countdown work when the application window is closing. */
    public void shutdown() {
        Thread activeLogin = loginThread.getAndSet(null);
        if (activeLogin != null) {
            activeLogin.interrupt();
        }
        stopCountdown();
        Thread activeDemo = demoThread.getAndSet(null);
        if (activeDemo != null) {
            activeDemo.interrupt();
        }
    }

    private void showResult(String email, LoginTask.Result result) {
        loginThread.set(null);
        switch (result.status()) {
            case SUCCESS -> {
                errorLabel.setVisible(false);
                showWelcome();
            }
            case INVALID -> {
                if (result.attemptsRemaining() >= 0) {
                    errorLabel.setText("Invalid username or password. Attempts remaining: "
                            + result.attemptsRemaining());
                } else {
                    errorLabel.setText("Invalid username or password");
                }
                errorLabel.setVisible(true);
                setControlsDisabled(false);
            }
            case LOCKED -> {
                errorLabel.setText("Account locked. Try again in "
                        + result.remainingLockoutSeconds() + " seconds.");
                errorLabel.setVisible(true);
                startCountdown(email);
                setControlsDisabled(true);
            }
            case ERROR -> {
                errorLabel.setText("Login could not be completed. Please try again.");
                errorLabel.setVisible(true);
                setControlsDisabled(false);
            }
        }
    }

    private void startCountdown(String email) {
        stopCountdown();
        Thread taskThread = new Thread(new CountdownTask(attemptTracker, email, remainingSeconds -> {
            if (!email.equals(nameField.getText())) {
                return;
            }
            if (remainingSeconds == 0) {
                errorLabel.setVisible(false);
                stopCountdown();
                setControlsDisabled(false);
            } else {
                errorLabel.setText("Account locked. Try again in "
                        + remainingSeconds + " seconds.");
                errorLabel.setVisible(true);
            }
        }), "lockout-countdown");
        taskThread.setDaemon(true);
        countdownThread.set(taskThread);
        taskThread.start();
    }

    private void stopCountdown() {
        Thread activeCountdown = countdownThread.getAndSet(null);
        if (activeCountdown != null) {
            activeCountdown.interrupt();
        }
    }

    private void setControlsDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        passField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }

    /** Loads the welcome FXML and displays it in the current window. */
    private void showWelcome() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/users/welcome-view.fxml"));
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Welcome");
            stage.show();
        } catch (IOException exception) {
            exception.printStackTrace();
            setControlsDisabled(false);
        }
    }
}
