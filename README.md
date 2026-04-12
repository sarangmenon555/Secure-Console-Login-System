# Secure Console Login System

A command-line user authentication system written in Kotlin, featuring secure password hashing with salting, account lockout protection, persistent file-based storage, and a full activity log.

---

## Features

- User registration with email validation
- Secure login with SHA-256 password hashing + per-user random salt
- Account lockout after 3 failed login attempts
- Password change (requires current password verification)
- Account deletion (requires password confirmation)
- Password recovery simulation
- Persistent user storage in a plain text file
- Timestamped activity log for all events

---

## Requirements

- **Language:** Kotlin
- **JDK:** Java 8 or higher
- **Build Tool:** Any (can run with `kotlinc` or via IntelliJ IDEA / Gradle)

---

## Project Structure

```
secure-console-login/
├── Main.kt           # Entry point + SecureConsoleUserManager class
├── users.txt         # Auto-generated — stores registered users (hashed)
├── log.txt           # Auto-generated — stores timestamped activity logs
└── README.md
```

> `users.txt` and `log.txt` are created automatically at runtime in the working directory.

---

## Menu Options

```
=== Secure Console Login ===
1. Register
2. Login
3. Forgot Password
4. View Logs
5. Exit
```

After a successful login, account management options are available:

```
-- Account Options --
1. Change Password
2. Delete Account
3. Logout
```

---

## Security Details

| Feature                  | Implementation                                      |
|--------------------------|-----------------------------------------------------|
| Password hashing         | SHA-256 via `java.security.MessageDigest`           |
| Salting                  | 16-byte random salt per user via `SecureRandom`     |
| Salt storage             | Stored alongside hash in `users.txt` (Base64)       |
| Account lockout          | Locks after 3 consecutive failed login attempts     |
| Email validation         | Regex-based format check before any operation       |
| Password change          | Requires current password before updating           |
| Account deletion         | Requires password confirmation before removal       |

---

## Data Storage Format

### `users.txt`
Each line stores one user record in the format:

```
email:salt:hashedPassword
```

Example:
```
alice@example.com:dGhpcyBpcyBh:5e884898da28047151d0e56f8dc629...
```

### `log.txt`
Timestamped log of all authentication events:

```
[2025-04-12 14:30:01] Registered: alice@example.com
[2025-04-12 14:30:45] Login success: alice@example.com
[2025-04-12 14:31:10] Password changed: alice@example.com
[2025-04-12 14:55:02] Login failed: bob@example.com
[2025-04-12 14:55:10] Locked out: bob@example.com
```

---

## Known Limitations

- **No real password recovery** — the forgot password flow is simulated (prints a message only; no email is sent).
- **Account lockout is session-only** — failed attempt counters reset when the program restarts, as they are stored in memory only.
- **Plain text file storage** — suitable for learning/demo purposes; a production system should use a proper database.
- **Passwords entered in plaintext** — the console does not mask password input (a `Console.readPassword()` integration would improve this).

---

## Example Session

```
=== Secure Console Login ===
1. Register
Choose an option: 1
Enter email: alice@example.com
Enter password: mysecurepass
Registration successful.

Choose an option: 2
Enter email: alice@example.com
Enter password: mysecurepass
✅ Login successful. Welcome, alice@example.com!

-- Account Options --
1. Change Password
Select: 1
Enter current password: mysecurepass
Enter new password: newpass123
Password changed successfully.
