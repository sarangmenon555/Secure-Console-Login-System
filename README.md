# Secure Console Login System

A command-line user authentication system written in Kotlin, featuring secure password hashing
with PBKDF2, persistent account lockout, atomic file writes, masked password input, and a full
activity log.

---

## Features

- User registration with email validation and password strength enforcement
- Secure login with PBKDF2WithHmacSHA256 (310,000 iterations) + per-user random salt
- Account lockout after 3 failed login attempts, persisted across restarts
- Password change (requires current password verification, enforces new password strength)
- Account deletion (requires password confirmation)
- Password recovery via cryptographic reset token
- Persistent user storage with atomic writes to prevent corruption on crash
- Masked password input via System.console().readPassword()
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
├── Main.kt   
├── users.txt    
├── log.txt        
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

| Feature              | Implementation                                                        |
|----------------------|-----------------------------------------------------------------------|
| Password hashing     | PBKDF2WithHmacSHA256 via `SecretKeyFactory` (310,000 iterations)     |
| Key length           | 256-bit derived key                                                   |
| Salting              | 16-byte random salt per user via `SecureRandom`                      |
| Salt storage         | Stored alongside hash in `users.txt` (Base64)                        |
| Account lockout      | Locks after 3 consecutive failed attempts, persisted to disk         |
| Email validation     | Regex-based format check before any operation                        |
| Password strength    | Min 8 chars, requires uppercase, lowercase, digit, special character |
| Password change      | Requires current password; new password must differ                  |
| Account deletion     | Requires password confirmation before removal                        |
| Password input       | Masked via `System.console().readPassword()` where supported         |
| Memory safety        | Password char array cleared via `PBEKeySpec.clearPassword()`         |
| File write safety    | Writes to `.tmp` file then renames atomically                        |

---

## Data Storage Format

### `users.txt`

Each line stores one user record in the format:

```
email:salt:hashedPassword:failedAttempts
```

Example:

```
alice@example.com:dGhpcyBpcyBh:5e884898da28047151d0e56f8dc629...:0
```

### `log.txt`

Timestamped log of all authentication events:

```
[2025-04-12 14:30:01] Registered: alice@example.com
[2025-04-12 14:30:45] Login success: alice@example.com
[2025-04-12 14:31:10] Password changed: alice@example.com
[2025-04-12 14:55:02] Login failed: bob@example.com
[2025-04-12 14:55:10] Locked out attempt: bob@example.com
```

---

## Known Limitations

- **No real email delivery** — the forgot password flow generates a cryptographic token but
  prints it to the console and logs it; a production system would email it instead.
- **Plain text file storage** — suitable for learning/demo purposes; a production system
  should use a proper database.
- **Password masking fallback** — if `System.console()` is unavailable (e.g. when running
  inside some IDEs), passwords fall back to unmasked `readLine()` input.
