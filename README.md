# SoftwareEngineeringLabs
University Project for Software Engineering Course

## Requirements

- JDK 21 (set `JAVA_HOME` to its installation folder if necessary).
- Scene Builder 21 or newer, installed separately, for editing the screen visually.
- No separate Maven or JavaFX SDK installation is required: the Maven Wrapper
  downloads Maven, and Maven downloads the JavaFX dependencies. The first build
  requires internet access.

## Open the project

In VS Code, choose **File > Open Folder** and open this repository's root folder
(the one containing `pom.xml`), not an individual Java file. Install the
**Extension Pack for Java** if it is not already installed, and allow Maven
project import to finish.

In IntelliJ IDEA, open `pom.xml` as a project. In Eclipse, use
**File > Import > Maven > Existing Maven Projects** and select this folder.

## Edit the screen in Scene Builder

1. Start Scene Builder and choose **File > Open**.
2. Open `src\main\resources\com\example\users\users-view.fxml`.
3. Drag controls onto the layout, edit their properties, and save the FXML file.
4. Run (or restart) the graphical application using the command below.

The layouts are in FXML and are handled by `LoginController`. The application
reads `users.txt` off the JavaFX application thread, then creates and updates
the screen on the JavaFX thread. Scene Builder edits the FXML; login behavior
and background work are implemented in Java.

## Run the Lab 3 graphical application

The application requires two positive integer arguments: `n` is the maximum
number of consecutive failed attempts for one known email, and `t` is the
lockout duration in seconds. Missing, invalid, or non-positive values are
requested again in the console before JavaFX starts.

From a terminal in the repository root (PowerShell or CMD), pass the arguments
to the JavaFX Maven plugin:

```bat
.\mvnw.cmd javafx:run -Djavafx.args="3 30"
```

For an IDE run configuration, set **Program arguments** to `3 30`. The login
screen disables its inputs while processing each attempt. Known emails are
tracked independently in memory; unknown emails receive the same generic
invalid-credentials message and are not tracked. A lockout remains in effect
even when the correct password is entered, and its remaining time is displayed.

`LoginTask` and `CountdownTask` implement `Runnable`, separating work from the
thread used to run it; this is useful when task logic should not be tied to a
particular thread and can be run by different thread mechanisms. `BlockCheckThread`
and `FailureWorker` extend `Thread` to provide the dedicated thread types
required by the lab. Two daemon failure workers consume events from a shared,
capacity-one `BlockingQueue`. The per-email `UserState` synchronizes access to
counters and lockout times. The login task joins the lock-check thread, and
`CountdownTask` sleeps between updates rather than busy-waiting. These threads
handle interruption during shutdown. Background tasks return UI changes through
`Platform.runLater`, keeping JavaFX updates on the application thread.

## Run tests

```bat
.\mvnw.cmd clean verify
```

Tests load a separate `src\test\resources\users.txt`, exercise valid and
invalid login attempts and lockout behavior, and use concurrent failures to
check the attempt threshold. `ConcurrencyDemonstrationTest` also coordinates
two unsynchronized increments to demonstrate a lost update, then verifies that
the synchronized user state preserves both updates.
The login screen also has a **Run concurrency demonstration** button. Enter a
known email first; it runs 20 threads against an isolated copy of that account's
state, checks that the account is not locked at `n - 1` failures and is locked
at `n`, and leaves the live account unchanged. It also compares 100 counter
increments per thread with and without synchronization. Results are displayed
on screen after the background work completes.

For the lab submission, keep the Lab 2 and Lab 3 branches separate. Make at
least four meaningful commits on the Lab 3 branch and capture the requested
commit-history screenshots manually.

## Run the existing console application

The original `User` and `UsersApp` classes are unchanged and now live under
`src\main\java`, following Maven's standard source layout. They remain in the
default package to preserve the `UsersApp` command.

Build, then run from the repository root:

```bat
.\mvnw.cmd clean verify
java -cp target\classes UsersApp > out1.txt
```

The program reads `users.txt` from the working directory. Sorted users are written
to standard output with LF (`\n`) line endings on every platform so redirected
output can be compared byte-for-byte with the expected `out.txt`. Diagnostics
remain on standard error and are not included in `out1.txt`.

For byte-for-byte redirected output, use CMD (Windows PowerShell 5 can re-encode
native output), or run this command from PowerShell:

```powershell
cmd /c "java -cp target\classes UsersApp > out1.txt"
```

Keep `users.txt` in the repository root; it is external input, not a bundled
application resource. If you have not supplied it, the console program reports
a file-read error.

`target` contains generated build output and is ignored by Git.