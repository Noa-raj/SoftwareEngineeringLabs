import java.util.function.IntConsumer;

import javafx.application.Platform;

/**
 * Updates the login screen with the remaining lockout time without busy-waiting.
 */
public class CountdownTask implements Runnable {
    private final AttemptTracker tracker;
    private final String email;
    private final IntConsumer update;

    public CountdownTask(AttemptTracker tracker, String email, IntConsumer update) {
        this.tracker = tracker;
        this.email = email;
        this.update = update;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                int remainingSeconds = tracker.getRemainingLockoutSeconds(email);
                Platform.runLater(() -> update.accept(remainingSeconds));
                if (remainingSeconds == 0) {
                    return;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
