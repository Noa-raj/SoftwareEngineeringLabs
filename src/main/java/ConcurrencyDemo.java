import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Demonstrates lost updates and verifies a concurrent login lockout threshold.
 */
public final class ConcurrencyDemo {
    private ConcurrencyDemo() {
    }

    /**
     * Runs the same number of increments from multiple threads with and without synchronization.
     * Workers coordinate each round so they all read before writing.
     */
    public static Result run(int threadCount, int incrementsPerThread) throws Exception {
        if (threadCount < 2 || incrementsPerThread < 1) {
            throw new IllegalArgumentException("Use at least two threads and one increment");
        }

        int[] unsynchronizedValue = {0};
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        CyclicBarrier allRead = new CyclicBarrier(threadCount);
        CyclicBarrier allWrote = new CyclicBarrier(threadCount);
        CyclicBarrier synchronizedRound = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                tasks.add(executor.submit(() -> {
                    for (int round = 0; round < incrementsPerThread; round++) {
                        int previousValue = unsynchronizedValue[0];
                        await(allRead);
                        unsynchronizedValue[0] = previousValue + 1;
                        await(allWrote);

                        synchronizedCounter.increment();
                        await(synchronizedRound);
                    }
                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
            return new Result(threadCount, incrementsPerThread,
                    unsynchronizedValue[0], synchronizedCounter.getValue());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies the configured lockout boundary with concurrent failures for one known email.
     * An isolated tracker ensures the live application's lockout state is unchanged.
     */
    public static LoginThresholdResult runLoginThreshold(AttemptTracker source, String email)
            throws Exception {
        User user = source.findUser(email);
        if (user == null) {
            throw new IllegalArgumentException("Enter an email address from users.txt first");
        }

        int threshold = source.getMaximumAttempts();
        AttemptTracker isolated = new AttemptTracker(List.of(user), threshold,
                source.getLockoutSeconds());
        isolated.startWorkers();
        try {
            int beforeThreshold = threshold - 1;
            runConcurrentAttempts(isolated, email, 20, beforeThreshold);
            int countBeforeThreshold = isolated.getUserState(email).getFailedAttempts();
            int remainingBeforeThreshold = isolated.getRemainingLockoutSeconds(email);
            UserState.FailureResult thresholdResult = isolated.recordFailure(email);

            return new LoginThresholdResult(threshold, beforeThreshold, countBeforeThreshold,
                    remainingBeforeThreshold == 0, thresholdResult.locked(),
                    thresholdResult.remainingLockoutSeconds());
        } finally {
            isolated.close();
        }
    }

    private static void runConcurrentAttempts(AttemptTracker tracker, String email,
                                              int threadCount, int attempts)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier start = new CyclicBarrier(threadCount);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                int attemptsForThread = attempts / threadCount
                        + (i < attempts % threadCount ? 1 : 0);
                tasks.add(executor.submit(() -> {
                    await(start);
                    for (int attempt = 0; attempt < attemptsForThread; attempt++) {
                        tracker.recordFailure(email);
                    }
                    return null;
                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency demonstration was interrupted",
                    exception);
        } catch (BrokenBarrierException exception) {
            throw new IllegalStateException("Concurrency demonstration barrier failed", exception);
        }
    }

    private static final class SynchronizedCounter {
        private int value;

        synchronized void increment() {
            value++;
        }

        synchronized int getValue() {
            return value;
        }
    }

    public record Result(int threadCount, int incrementsPerThread, int unsynchronizedCount,
                         int synchronizedCount) {
        public int expectedCount() {
            return threadCount * incrementsPerThread;
        }
    }

    public record LoginThresholdResult(int threshold, int attemptsBeforeThreshold,
                                       int observedFailures, boolean notLockedBeforeThreshold,
                                       boolean lockedAtThreshold,
                                       int remainingLockoutSeconds) {
    }
}
