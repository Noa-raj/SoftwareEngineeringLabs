/**
 * Synchronized, in-memory login state for one known email address.
 */
public class UserState {
    private int failedAttempts;
    private long lockoutUntilMillis;

    /**
     * Returns the number of whole seconds remaining in the lockout, or zero when unlocked.
     * Expired lockouts reset the failed-attempt count.
     */
    public synchronized int getRemainingLockoutSeconds() {
        if (lockoutUntilMillis == 0) {
            return 0;
        }

        long remainingMillis = lockoutUntilMillis - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            lockoutUntilMillis = 0;
            failedAttempts = 0;
            return 0;
        }
        return (int) ((remainingMillis + 999) / 1000);
    }

    /**
     * Records one failed login unless the account is already locked.
     */
    public synchronized FailureResult recordFailure(int maximumAttempts, int lockoutSeconds) {
        int remainingLockout = getRemainingLockoutSeconds();
        if (remainingLockout > 0) {
            return new FailureResult(true, remainingLockout, 0);
        }

        failedAttempts++;
        if (failedAttempts >= maximumAttempts) {
            lockoutUntilMillis = System.currentTimeMillis() + lockoutSeconds * 1000L;
            return new FailureResult(true, lockoutSeconds, 0);
        }
        return new FailureResult(false, 0, maximumAttempts - failedAttempts);
    }

    /** Resets both the failed-attempt count and any active lockout. */
    public synchronized void resetAfterSuccess() {
        failedAttempts = 0;
        lockoutUntilMillis = 0;
    }

    /** Returns the current failed-attempt count, primarily for diagnostics and tests. */
    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }

    /** Outcome of applying one failed attempt to this user's state. */
    public record FailureResult(boolean locked, int remainingLockoutSeconds,
                                int attemptsRemaining) {
    }
}
