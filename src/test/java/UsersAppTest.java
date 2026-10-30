import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UsersAppTest {
    @TempDir
    Path directory;

    @Test
    void sortsUsersPreservingDuplicatesAndLfLineEndings() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new User("z@example.com", "Abcd123!"));
        users.add(new User("a@example.com", "Abcd123!"));
        users.add(new User("a@example.com", "Abcd123!"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            UsersApp.printSortedUsers(users, output);
        }

        assertEquals("a@example.com Abcd123!\na@example.com Abcd123!\nz@example.com Abcd123!\n",
                bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void readsValidUsersAndReportsInvalidLinesSeparately() throws Exception {
        Path input = directory.resolve("users.txt");
        Files.writeString(input, "valid@example.com Abcd123!\ninvalid Abcd123!\n");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ArrayList<User> users;
        try (PrintStream diagnostics = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            users = UsersApp.readUsers(input, diagnostics);
        }

        assertEquals(1, users.size());
        assertEquals("valid@example.com", users.get(0).getUsername());
        assertEquals("invalid Abcd123! : Please enter a valid Email as username"
                + System.lineSeparator(), bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void reportsMissingInputFileToCaller() {
        assertThrows(java.io.IOException.class,
                () -> UsersApp.readUsers(directory.resolve("missing.txt"), System.err));
    }

    @Test
    void acceptsLengthBoundariesAndReportsMalformedFileRows() throws Exception {
        String usernameAtLimit = "a".repeat(38) + "@example.com";
        String usernameOverLimit = "a".repeat(39) + "@example.com";
        Path input = directory.resolve("edge-users.txt");
        List<String> lines = List.of(
                usernameAtLimit + " A1!bcdef",
                "twelve@example.com Abcdef12!@#$",
                "multiple-spaces@example.com   Abcd123!   ",
                usernameOverLimit + " Abcd123!",
                "short-password@example.com A1!bcde",
                "long-password@example.com Abcdef12!@#$X",
                "no-letter@example.com 1234567!",
                "no-digit@example.com Abcdefg!",
                "no-symbol@example.com Abcdef12",
                "space@example.com Abcd 123!",
                "unicode@example.com Abcd123é!",
                "invalid-email Abcd123!",
                "",
                " leading@example.com Abcd123!",
                "only-one-field@example.com",
                "extra@example.com Abcd123! extra",
                "tab@example.com\tAbcd123!");
        Files.writeString(input, String.join("\n", lines));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ArrayList<User> users;
        try (PrintStream diagnostics = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            users = UsersApp.readUsers(input, diagnostics);
        }

        assertEquals(List.of(usernameAtLimit, "twelve@example.com",
                        "multiple-spaces@example.com"),
                users.stream().map(User::getUsername).toList());
        String diagnostics = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(diagnostics.contains(usernameOverLimit
                + " Abcd123! : Username is too long, try something shorter"));
        assertTrue(diagnostics.contains("short-password@example.com A1!bcde"
                + " : Your password is too short, add more characters"));
        assertTrue(diagnostics.contains("long-password@example.com Abcdef12!@#$X"
                + " : Your password is too long, try a shorter one"));
        assertTrue(diagnostics.contains("no-letter@example.com 1234567!"
                + " : Please enter a valid password"));
        assertTrue(diagnostics.contains("no-digit@example.com Abcdefg!"
                + " : Please enter a valid password"));
        assertTrue(diagnostics.contains("no-symbol@example.com Abcdef12"
                + " : Please enter a valid password"));
        assertTrue(diagnostics.contains("space@example.com Abcd 123!"
                + " : Expected a username and password separated by spaces"));
        assertTrue(diagnostics.contains("unicode@example.com Abcd123é!"
                + " : Please enter a valid password"));
        assertTrue(diagnostics.contains("invalid-email Abcd123!"
                + " : Please enter a valid Email as username"));
        assertTrue(diagnostics.contains(" : Please enter a valid Email as username"));
        assertTrue(diagnostics.contains(" leading@example.com Abcd123!"
                + " : Please enter a valid Email as username"));
        assertTrue(diagnostics.contains("only-one-field@example.com"
                + " : Expected a username and password separated by spaces"));
        assertTrue(diagnostics.contains("extra@example.com Abcd123! extra"
                + " : Expected a username and password separated by spaces"));
        assertTrue(diagnostics.contains("tab@example.com\tAbcd123!"
                + " : Expected a username and password separated by spaces"));
    }

    @Test
    void readsLargeUserFileWithoutDroppingValidRecords() throws Exception {
        Path input = directory.resolve("large-users.txt");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            lines.add(String.format("user%04d@example.test Pass%04d!", i, i));
        }
        Files.writeString(input, String.join("\n", lines));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ArrayList<User> users;
        try (PrintStream diagnostics = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            users = UsersApp.readUsers(input, diagnostics);
        }

        assertEquals(1_000, users.size());
        assertEquals("user0000@example.test", users.get(0).getUsername());
        assertEquals("Pass0000!", users.get(0).getPassword());
        assertEquals("user0999@example.test", users.get(999).getUsername());
        assertEquals("Pass0999!", users.get(999).getPassword());
        assertEquals("", bytes.toString(StandardCharsets.UTF_8));
    }
}
