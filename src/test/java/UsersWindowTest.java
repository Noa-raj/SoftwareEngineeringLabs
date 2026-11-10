import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Scanner;

import org.junit.jupiter.api.Test;

class UsersWindowTest {
    @Test
    void acceptsValidCommandLineParameterWithoutPrompting() {
        assertEquals(4, UsersWindow.readPositiveArgument(
                new String[]{"4"}, 0, "n", new Scanner("")));
    }

    @Test
    void repromptsForMissingNonNumericAndNonPositiveParameters() {
        assertEquals(3, UsersWindow.readPositiveArgument(
                new String[0], 1, "t", new Scanner("invalid\n0\n3\n")));
    }
}
