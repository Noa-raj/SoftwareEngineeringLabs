import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConcurrencyDemoTest {
    @Test
    void showsLostUpdatesWithoutSynchronizationAndPreservesAllWithIt() throws Exception {
        ConcurrencyDemo.Result result = ConcurrencyDemo.run(8, 25);

        assertEquals(200, result.expectedCount());
        assertEquals(25, result.unsynchronizedCount());
        assertEquals(200, result.synchronizedCount());
    }

    @Test
    void concurrentlyChecksActualLoginThresholdWithoutChangingLiveAccountState()
            throws Exception {
        User user = new User("demo@example.com", "Abcd123!");
        AttemptTracker liveTracker = new AttemptTracker(List.of(user), 257, 3);
        try {
            ConcurrencyDemo.LoginThresholdResult result =
                    ConcurrencyDemo.runLoginThreshold(liveTracker, "demo@example.com");

            assertEquals(256, result.attemptsBeforeThreshold());
            assertEquals(256, result.observedFailures());
            assertTrue(result.notLockedBeforeThreshold());
            assertTrue(result.lockedAtThreshold());
            assertTrue(result.remainingLockoutSeconds() > 0);
            assertEquals(0, liveTracker.getUserState("demo@example.com").getFailedAttempts());
            assertFalse(liveTracker.getUserState("demo@example.com")
                    .getRemainingLockoutSeconds() > 0);
        } finally {
            liveTracker.close();
        }
    }

    @Test
    void rejectsUnknownEmailForTheLoginThresholdDemo() {
        AttemptTracker tracker = new AttemptTracker(
                List.of(new User("known@example.com", "Abcd123!")), 3, 1);
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> ConcurrencyDemo.runLoginThreshold(tracker, "unknown@example.com"));
        } finally {
            tracker.close();
        }
    }
}
