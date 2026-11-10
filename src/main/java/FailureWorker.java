import java.util.concurrent.ArrayBlockingQueue;

/**
 * Processes failed-login events for one email on a daemon thread.
 */
public class FailureWorker extends Thread {
    private final ArrayBlockingQueue<FailedAttempt> queue;
    private final UserState userState;
    private final int maximumAttempts;
    private final int lockoutSeconds;

    FailureWorker(String email, ArrayBlockingQueue<FailedAttempt> queue, UserState userState,
                  int maximumAttempts, int lockoutSeconds) {
        super("failure-worker-" + email);
        this.queue = queue;
        this.userState = userState;
        this.maximumAttempts = maximumAttempts;
        this.lockoutSeconds = lockoutSeconds;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            FailedAttempt attempt = null;
            try {
                attempt = queue.take();
                UserState.FailureResult result =
                        userState.recordFailure(maximumAttempts, lockoutSeconds);
                attempt.response().complete(result);
            } catch (InterruptedException exception) {
                if (attempt != null) {
                    attempt.response().completeExceptionally(exception);
                }
                interrupt();
            }
        }
    }
}
