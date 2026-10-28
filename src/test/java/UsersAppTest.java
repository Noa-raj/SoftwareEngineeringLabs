import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
}
