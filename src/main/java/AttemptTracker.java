import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Owns per-email login state, bounded failure queues, and their daemon workers.
 */
public class AttemptTracker implements AutoCloseable {
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, UserState> states = new ConcurrentHashMap<>();
    private final ArrayBlockingQueue<FailedAttempt> queue = new ArrayBlockingQueue<>(1);
    private final List<FailureWorker> workers = new ArrayList<>();
    private final int maximumAttempts;
    private final int lockoutSeconds;
    private volatile boolean started;

    /**
     * Creates tracked state and a capacity-one queue for each distinct known email.
     *
     * @param users valid users loaded from the users file
     * @param maximumAttempts failed attempts allowed before lockout
     * @param lockoutSeconds lockout duration
     */
    public AttemptTracker(List<User> users, int maximumAttempts, int lockoutSeconds) {
        if (maximumAttempts < 1 || lockoutSeconds < 1) {
            throw new IllegalArgumentException("n and t must both be at least 1");
        }
        this.maximumAttempts = maximumAttempts;
        this.lockoutSeconds = lockoutSeconds;

        for (User user : users) {
            String email = user.getUsername();
            if (usersByEmail.putIfAbsent(email, user) == null) {
                UserState state = new UserState();
                states.put(email, state);
            }
        }
    }

    /** Starts one daemon failure worker for each known email. */
    public synchronized void startWorkers() {
        if (started) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            FailureWorker worker = new FailureWorker(i + 1, queue);
            workers.add(worker);
            worker.start();
        }
        started = true;
    }

    /** Returns the known user for an exact email match, or {@code null} if it is unknown. */
    public User findUser(String email) {
        return usersByEmail.get(email);
    }

    /** Returns synchronized state for a known email, or {@code null} for an unknown email. */
    public UserState getUserState(String email) {
        return states.get(email);
    }

    /** Returns the configured failed-attempt threshold. */
    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    /** Returns the configured lockout duration in seconds. */
    public int getLockoutSeconds() {
        return lockoutSeconds;
    }

    int getFailureWorkerCount() {
        return workers.size();
    }

    /**
     * Sends a failed attempt through that email's bounded queue and waits for its worker's result.
     */
    public UserState.FailureResult recordFailure(String email)
            throws InterruptedException, ExecutionException {
        if (!started) {
            throw new IllegalStateException("Failure workers have not been started");
        }
        UserState userState = states.get(email);
        if (userState == null) {
            throw new IllegalArgumentException("Cannot track an unknown email");
        }

        CompletableFuture<UserState.FailureResult> response = new CompletableFuture<>();
        queue.put(new FailedAttempt(userState, maximumAttempts, lockoutSeconds, response));
        return response.get();
    }

    /** Returns the current lockout time for a known email, or zero for an unknown email. */
    public int getRemainingLockoutSeconds(String email) {
        UserState state = states.get(email);
        return state == null ? 0 : state.getRemainingLockoutSeconds();
    }

    /** Interrupts all background workers so they stop blocking on their queues. */
    @Override
    public synchronized void close() {
        workers.forEach(Thread::interrupt);
        started = false;
    }
}
