import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConcurrencyDemoTest {
    @Test
    void showsLostUpdatesWithoutSynchronizationAndPreservesAllWithIt() throws Exception {
        ConcurrencyDemo.Result result = ConcurrencyDemo.run(8, 25);

        assertEquals(200, result.expectedCount());
        assertEquals(25, result.unsynchronizedCount());
        assertEquals(200, result.synchronizedCount());
    }
}
