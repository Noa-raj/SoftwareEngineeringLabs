import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

// קוראת משתמשים מקובץ, מדווחת על שורות שגויות ומדפיסה משתמשים תקינים במיון.
public class UsersApp {
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

    // ממיינת את הרשימה עצמה ומדפיסה כל משתמש בשורה נפרדת.
    static void printSortedUsers(ArrayList<User> users, PrintStream output) {
        // המיון לפי שם המשתמש רגיש לאותיות גדולות וקטנות; כפילויות נשמרות.
        Collections.sort(users, (u1, u2) -> u1.getUsername().compareTo(u2.getUsername()));
        for (User user : users) {
            output.println(user.getUsername() + " " + user.getPassword());
        }
    }

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
