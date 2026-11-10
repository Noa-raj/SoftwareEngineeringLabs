import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class AttemptTrackerTest {
    @Test
    void tracksConcurrentFailuresWithoutLosingAttemptsAtTheLimit() throws Exception {
        int maximumAttempts = 257;
        AttemptTracker tracker = trackerFor("parallel@example.com", maximumAttempts, 3);
        tracker.startWorkers();
        try {
            runConcurrentFailures(tracker, "parallel@example.com", 8, 30);
            assertEquals(240, tracker.getUserState("parallel@example.com").getFailedAttempts());
            assertEquals(0, tracker.getRemainingLockoutSeconds("parallel@example.com"));

            runConcurrentFailures(tracker, "parallel@example.com", 16, 1);
            assertEquals(256, tracker.getUserState("parallel@example.com").getFailedAttempts());
            assertEquals(0, tracker.getRemainingLockoutSeconds("parallel@example.com"));

            runConcurrentFailures(tracker, "parallel@example.com", 1, 1);
            assertTrue(tracker.getRemainingLockoutSeconds("parallel@example.com") > 0);
        } finally {
            tracker.close();
        }
    }

    @Test
    void ignoresFailuresDuringLockoutAndResetsAfterExpiryOrSuccess() throws Exception {
        AttemptTracker tracker = trackerFor("timed@example.com", 2, 1);
        tracker.startWorkers();
        try {
            UserState.FailureResult first = tracker.recordFailure("timed@example.com");
            assertFalse(first.locked());
            assertEquals(1, first.attemptsRemaining());

            UserState.FailureResult second = tracker.recordFailure("timed@example.com");
            assertTrue(second.locked());
            assertTrue(second.remainingLockoutSeconds() > 0);
            assertEquals(2, tracker.getUserState("timed@example.com").getFailedAttempts());

            UserState.FailureResult duringLockout = tracker.recordFailure("timed@example.com");
            assertTrue(duringLockout.locked());
            assertEquals(2, tracker.getUserState("timed@example.com").getFailedAttempts());

            Thread.sleep(1_100);
            UserState.FailureResult afterExpiry = tracker.recordFailure("timed@example.com");
            assertFalse(afterExpiry.locked());
            assertEquals(1, afterExpiry.attemptsRemaining());
            assertEquals(1, tracker.getUserState("timed@example.com").getFailedAttempts());

            tracker.getUserState("timed@example.com").resetAfterSuccess();
            assertEquals(0, tracker.getUserState("timed@example.com").getFailedAttempts());
        } finally {
            tracker.close();
        }
    }

    @Test
    void doesNotCreateTrackingStateForUnknownEmail() {
        AttemptTracker tracker = trackerFor("known@example.com", 3, 1);
        try {
            assertNull(tracker.findUser("unknown@example.com"));
            assertNull(tracker.getUserState("unknown@example.com"));
            assertEquals(0, tracker.getRemainingLockoutSeconds("unknown@example.com"));
        } finally {
            tracker.close();
        }
    }

    @Test
    void requiresPositiveAttemptAndLockoutLimits() {
        List<User> users = List.of(new User("known@example.com", "Abcd123!"));
        assertThrows(IllegalArgumentException.class, () -> new AttemptTracker(users, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new AttemptTracker(users, 1, 0));
    }

    private static AttemptTracker trackerFor(String email, int maximumAttempts,
                                             int lockoutSeconds) {
        ArrayList<User> users = new ArrayList<>();
        users.add(new User(email, "Abcd123!"));
        return new AttemptTracker(users, maximumAttempts, lockoutSeconds);
    }

    private static void runConcurrentFailures(AttemptTracker tracker, String email,
                                              int threadCount, int attemptsPerThread)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start");
                    }
                    for (int attempt = 0; attempt < attemptsPerThread; attempt++) {
                        tracker.recordFailure(email);
                    }
                    return null;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
