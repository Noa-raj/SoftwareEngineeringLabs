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

All layout is in FXML; `UsersWindow.java` only loads it and opens the window.
Scene Builder edits JavaFX FXML, so JavaFX is still the runtime dependency even
though you do not write Java layout code. This is a starter screen, not a GUI
implementation of the console program. Interactive actions would need a Java
controller and event handlers in addition to the Scene Builder design.

## Run the graphical application

From a terminal in the repository root (PowerShell or CMD):

```bat
.\mvnw.cmd javafx:run
```

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