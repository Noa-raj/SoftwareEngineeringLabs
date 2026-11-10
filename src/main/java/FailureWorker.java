import java.util.concurrent.ArrayBlockingQueue;

/**
 * Processes failed-login events from the shared bounded queue on a daemon thread.
 */
public class FailureWorker extends Thread {
    private final ArrayBlockingQueue<FailedAttempt> queue;

    FailureWorker(int workerNumber, ArrayBlockingQueue<FailedAttempt> queue) {
        super("failure-worker-" + workerNumber);
        this.queue = queue;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            FailedAttempt attempt = null;
            try {
                attempt = queue.take();
                UserState.FailureResult result =
                        attempt.userState().recordFailure(
                                attempt.maximumAttempts(), attempt.lockoutSeconds());
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
