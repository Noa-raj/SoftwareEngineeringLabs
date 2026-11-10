import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;

/**
 * Processes one login attempt away from the JavaFX application thread.
 */
public class LoginTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(LoginTask.class.getName());

    private final String email;
    private final String password;
    private final AttemptTracker tracker;
    private final Consumer<Result> completion;

    public LoginTask(String email, String password, AttemptTracker tracker,
                     Consumer<Result> completion) {
        this.email = email;
        this.password = password;
        this.tracker = tracker;
        this.completion = completion;
    }

    @Override
    public void run() {
        Result result;
        User user = tracker.findUser(email);
        if (user == null) {
            result = new Result(Status.INVALID, 0, -1);
        } else {
            UserState userState = tracker.getUserState(email);
            BlockCheckThread blockCheck = new BlockCheckThread(userState);
            try {
                blockCheck.start();
                blockCheck.join();

                int remainingSeconds = blockCheck.getRemainingSeconds();
                if (remainingSeconds > 0) {
                    result = new Result(Status.LOCKED, remainingSeconds, 0);
                } else {
                    synchronized (userState) {
                        remainingSeconds = userState.getRemainingLockoutSeconds();
                        if (remainingSeconds > 0) {
                            result = new Result(Status.LOCKED, remainingSeconds, 0);
                        } else if (user.getPassword().equals(password)) {
                            userState.resetAfterSuccess();
                            result = new Result(Status.SUCCESS, 0, 0);
                        } else {
                            result = null;
                        }
                    }
                    if (result == null) {
                        UserState.FailureResult failure = tracker.recordFailure(email);
                        result = failure.locked()
                                ? new Result(Status.LOCKED, failure.remainingLockoutSeconds(), 0)
                                : new Result(Status.INVALID, 0, failure.attemptsRemaining());
                    }
                }
            } catch (InterruptedException exception) {
                blockCheck.interrupt();
                Thread.currentThread().interrupt();
                result = new Result(Status.ERROR, 0, -1);
            } catch (ExecutionException exception) {
                LOGGER.log(Level.SEVERE, "Could not process failed login for " + email,
                        exception.getCause());
                result = new Result(Status.ERROR, 0, -1);
            }
        }

        Result completedResult = result;
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        Platform.runLater(() -> completion.accept(completedResult));
    }

    public enum Status {
        SUCCESS,
        INVALID,
        LOCKED,
        ERROR
    }

    public record Result(Status status, int remainingLockoutSeconds, int attemptsRemaining) {
    }
}
