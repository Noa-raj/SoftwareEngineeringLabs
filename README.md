# SoftwareEngineeringLabs
University Project for Software Engineering Course

Compile and run from CMD in the project directory:

```bat
javac User.java UsersApp.java
java UsersApp > out1.txt
```

The program reads `users.txt` from the working directory. Sorted users are written
to standard output with LF (`\n`) line endings on every platform so redirected
output can be compared byte-for-byte with the expected `out.txt`. Diagnostics
remain on standard error and are not included in `out1.txt`.

test