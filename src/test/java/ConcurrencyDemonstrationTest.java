import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ConcurrencyDemonstrationTest {
    @Test
    void synchronizedStateKeepsBothUpdatesWhileUnsynchronizedReadModifyWriteLosesOne()
            throws Exception {
        int[] unsynchronizedCount = {0};
        CountDownLatch bothThreadsRead = new CountDownLatch(2);
        CountDownLatch allowWrites = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> unsynchronizedIncrement(
                    unsynchronizedCount, bothThreadsRead, allowWrites));
            Future<?> second = executor.submit(() -> unsynchronizedIncrement(
                    unsynchronizedCount, bothThreadsRead, allowWrites));
            bothThreadsRead.await();
            allowWrites.countDown();
            first.get();
            second.get();

            assertEquals(1, unsynchronizedCount[0],
                    "The coordinated unsynchronized increments demonstrate a lost update");

            UserState synchronizedState = new UserState();
            CountDownLatch startTogether = new CountDownLatch(1);
            Future<?> synchronizedFirst = executor.submit(() -> {
                await(startTogether);
                synchronizedState.recordFailure(10, 1);
            });
            Future<?> synchronizedSecond = executor.submit(() -> {
                await(startTogether);
                synchronizedState.recordFailure(10, 1);
            });
            startTogether.countDown();
            synchronizedFirst.get();
            synchronizedSecond.get();
            assertEquals(2, synchronizedState.getFailedAttempts());
        } finally {
            executor.shutdownNow();
        }
    }

    private static void unsynchronizedIncrement(int[] count, CountDownLatch bothRead,
                                               CountDownLatch allowWrite) {
        int previous = count[0];
        bothRead.countDown();
        await(allowWrite);
        count[0] = previous + 1;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test latch timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test was interrupted", exception);
        }
    }
}
