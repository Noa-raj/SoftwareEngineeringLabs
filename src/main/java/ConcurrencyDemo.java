import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Compares a coordinated unsynchronized counter with a synchronized counter.
 */
public final class ConcurrencyDemo {
    private ConcurrencyDemo() {
    }

    /**
     * Runs the same number of increments from multiple threads with and without synchronization.
     * The unsynchronized workers are coordinated so they all read a round's value before writing.
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
}
