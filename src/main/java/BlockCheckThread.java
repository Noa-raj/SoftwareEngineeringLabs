/**
 * Checks whether a known user's lockout is still active.
 */
public class BlockCheckThread extends Thread {
    private final UserState userState;
    private int remainingSeconds;

    public BlockCheckThread(UserState userState) {
        super("block-check");
        this.userState = userState;
    }

    @Override
    public void run() {
        remainingSeconds = userState.getRemainingLockoutSeconds();
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}
