import java.util.concurrent.CompletableFuture;

/**
 * A failed login event and the response that its worker returns to the login task.
 */
record FailedAttempt(UserState userState, int maximumAttempts, int lockoutSeconds,
                     CompletableFuture<UserState.FailureResult> response) {
}
