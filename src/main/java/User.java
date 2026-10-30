import java.util.regex.Pattern;

/**
 * Represents a user whose username and password have passed validation.
 *
 * <p>The validation rules are enforced when an instance is created; invalid values cause an
 * {@link IllegalArgumentException}.</p>
 */
// מייצגת משתמש שנוצר רק לאחר אימות שם המשתמש והסיסמה.
public class User {
    // תבנית האימייל: חלק מקומי, דומיין שמתחיל באות או בספרה וסיומת של לפחות שתי אותיות.
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9][A-Za-z0-9.\\-]*\\.[A-Za-z]{2,}");
    // הסימנים המותרים בסיסמה, בנוסף לאותיות באנגלית ולספרות.
    private static final String PASSWORD_SYMBOLS = "`~!@#$%^&*()_+-=";

    // שדות פרטיים שאינם ניתנים להחלפה לאחר יצירת המשתמש.
    private final String username;
    private final String password;

    // אם אחד האימותים נכשל, נזרקת חריגה ויצירת המשתמש אינה מושלמת.
    /**
     * Creates a user after validating both supplied values.
     *
     * @param username the email-style username to validate
     * @param password the password to validate
     * @throws IllegalArgumentException if either value violates the validation rules
     */
    public User(String username, String password) {
        validateUsername(username);
        validatePassword(password);
        this.username = username;
        this.password = password;
    }

    // מחזירה את שם המשתמש לקריאה.
    /**
     * Returns this user's username.
     *
     * @return the validated username
     */
    public String getUsername() {
        return username;
    }

    // מחזירה את הסיסמה לצורך הפלט הנדרש בתרגיל.
    /**
     * Returns this user's password.
     *
     * @return the validated password
     */
    public String getPassword() {
        return password;
    }

    // בודקת ערך חסר, מגבלת אורך והתאמה מלאה לתבנית האימייל.
    private static void validateUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Please enter a valid Email as username");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("Username is too long, try something shorter");
        }
        if (!EMAIL_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Please enter a valid Email as username");
        }
    }

    // בודקת תחילה ערך חסר ואורך, ולאחר מכן תווים מותרים והרכב סיסמה.
    private static void validatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Please enter a valid password");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Your password is too short, add more characters");
        }
        if (password.length() > 12) {
            throw new IllegalArgumentException("Your password is too long, try a shorter one");
        }

        // הדגלים מתעדים אם נמצאו אות, ספרה וסימן במהלך סריקת הסיסמה.
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')) {
                hasLetter = true;
            } else if (character >= '0' && character <= '9') {
                hasDigit = true;
            } else if (PASSWORD_SYMBOLS.indexOf(character) >= 0) {
                hasSymbol = true;
            // כל תו שאינו שייך לאחת הקבוצות המותרות גורם לדחיית הסיסמה.
            } else {
                throw new IllegalArgumentException("Please enter a valid password");
            }
        }

        // סיסמה תקינה חייבת לכלול לפחות אות אחת, ספרה אחת וסימן אחד.
        if (!hasLetter || !hasDigit || !hasSymbol) {
            throw new IllegalArgumentException("Please enter a valid password");
        }
    }
}
