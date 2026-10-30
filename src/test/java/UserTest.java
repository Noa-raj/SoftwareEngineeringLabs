import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserTest {
    @Test
    void rejectsMissingOrMalformedUsernames() {
        assertInvalidUser(null, "Abcd123!", "Please enter a valid Email as username");
        assertInvalidUser("plain-text", "Abcd123!", "Please enter a valid Email as username");
        assertInvalidUser("name@example", "Abcd123!", "Please enter a valid Email as username");
        assertInvalidUser("name@example.c", "Abcd123!",
                "Please enter a valid Email as username");
        assertInvalidUser("@example.com", "Abcd123!",
                "Please enter a valid Email as username");
        assertInvalidUser("a".repeat(39) + "@example.com", "Abcd123!",
                "Username is too long, try something shorter");
    }

    @Test
    void acceptsEveryConfiguredPasswordSymbol() {
        String allowedSymbols = "`~!@#$%^&*()_+-=";
        for (int i = 0; i < allowedSymbols.length(); i++) {
            char symbol = allowedSymbols.charAt(i);
            User user = new User("user@example.com", "Aa12345" + symbol);
            assertEquals("Aa12345" + symbol, user.getPassword());
        }
    }

    @Test
    void rejectsMissingOrUnsupportedPasswordValues() {
        assertInvalidUser("user@example.com", null, "Please enter a valid password");
        assertInvalidUser("user@example.com", "A1!bcde",
                "Your password is too short, add more characters");
        assertInvalidUser("user@example.com", "Abcdef12!@#$X",
                "Your password is too long, try a shorter one");
        assertInvalidUser("user@example.com", "12345678",
                "Please enter a valid password");
        assertInvalidUser("user@example.com", "Abcdefgh",
                "Please enter a valid password");
        assertInvalidUser("user@example.com", "Abcd123:",
                "Please enter a valid password");
        assertInvalidUser("user@example.com", "Abcd123 ",
                "Please enter a valid password");
        assertInvalidUser("user@example.com", "Abcd123é!",
                "Please enter a valid password");
    }

    private static void assertInvalidUser(String username, String password,
                                          String expectedMessage) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new User(username, password));
        assertEquals(expectedMessage, exception.getMessage());
    }
}
