import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Reads user credentials from {@code users.txt}, reports invalid entries, and prints valid users.
 */
// קוראת משתמשים מקובץ, מדווחת על שורות שגויות ומדפיסה משתמשים תקינים במיון.
public class UsersApp {
    /**
     * Reads {@code users.txt} from the process working directory and prints valid users sorted by
     * username. File-read errors are reported to standard error.
     *
     * @param args command-line arguments; currently unused
     */
    public static void main(String[] args) {
        // הקובץ נפתח מתוך תיקיית העבודה שממנה מריצים את התוכנית.
        Path inputFile = Paths.get("users.txt");
        try {
            // הודעות שגיאה נשלחות לערוץ השגיאות; המשתמשים התקינים נשלחים לפלט הרגיל.
            ArrayList<User> users = readUsers(inputFile, System.err);
            printSortedUsers(users, System.out);
        // כשל בפתיחת הקובץ או בקריאתו מדווח לפלט השגיאות.
        } catch (IOException exception) {
            System.err.println("Could not read " + inputFile + ": " + exception.getMessage());
        }
    }

    /**
     * Sorts the supplied users by username and prints each username/password pair on its own line.
     *
     * @param users users to sort and print; this list is sorted in place
     * @param output destination for the formatted user records
     */
    // ממיינת את הרשימה עצמה ומדפיסה כל משתמש בשורה נפרדת.
    static void printSortedUsers(ArrayList<User> users, PrintStream output) {
        // המיון לפי שם המשתמש רגיש לאותיות גדולות וקטנות; כפילויות נשמרות.
        Collections.sort(users, (u1, u2) -> u1.getUsername().compareTo(u2.getUsername()));
        for (User user : users) {
            output.print(user.getUsername() + " " + user.getPassword() + "\n");
        }
    }

    /**
     * Reads user records from a file, keeping valid records and writing diagnostics for invalid
     * lines.
     *
     * <p>Each record is expected to contain a username and password separated by spaces. File
     * errors are not swallowed and are passed to the caller.</p>
     *
     * @param inputFile path to the input file
     * @param diagnostics destination for invalid-line messages
     * @return valid users in the order they appear in the file
     * @throws IOException if the file cannot be opened or read
     */
    // מחזירה רשימה של משתמשים תקינים בלבד; שגיאות קובץ מועברות למתודה הקוראת.
    static ArrayList<User> readUsers(Path inputFile, PrintStream diagnostics) throws IOException {
        ArrayList<User> users = new ArrayList<>();
        // הקורא נסגר אוטומטית בסיום הבלוק, גם במקרה של חריגה.
        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // שורה ריקה או רווח בתחילת השורה נחשבים לשם משתמש בפורמט שגוי.
                if (line.isEmpty() || line.startsWith(" ")) {
                    diagnostics.println(line + " : Please enter a valid Email as username");
                    continue;
                }
                // פיצול לפי רווח רגיל אחד או יותר. טאב אינו מפריד ורווחים בתחילת השורה אינם מוסרים.
                // פיצול זה משמיט שדות ריקים בסוף השורה, ולכן רווחים בסופה מתקבלים.
                String[] fields = line.split(" +");
                // נדרשים בדיוק שני שדות: שם משתמש וסיסמה.
                if (fields.length != 2) {
                    diagnostics.println(line
                            + " : Expected a username and password separated by spaces");
                    // מדלגים על יצירת משתמש מהשורה הפסולה וממשיכים לשורה הבאה.
                    continue;
                }

                try {
                    // ההוספה מתבצעת רק אם הבנאי השלים בהצלחה את כל בדיקות התקינות.
                    users.add(new User(fields[0], fields[1]));
                // מדפיסים את השורה המקורית ואת סיבת הדחייה וממשיכים בקריאת הקובץ.
                } catch (IllegalArgumentException exception) {
                    diagnostics.println(line + " : " + exception.getMessage());
                }
            }
        }
        return users;
    }
}
