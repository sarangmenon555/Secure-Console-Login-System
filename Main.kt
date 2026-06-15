import java.io.File
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

fun main() {
    val userManager = SecureConsoleUserManager("users.txt", "log.txt")

    while (true) {
        println("\n=== Secure Console Login ===")
        println("1. Register")
        println("2. Login")
        println("3. Forgot Password")
        println("4. View Logs")
        println("5. Exit")
        print("Choose an option: ")

        when (readLine()?.trim()) {
            "1" -> {
                print("Enter email: ")
                val email = readLine()?.trim() ?: ""
                val password = readPasswordFromConsole("Enter password: ")
                println(userManager.register(email, password))
            }

            "2" -> {
                print("Enter email: ")
                val email = readLine()?.trim() ?: ""
                val password = readPasswordFromConsole("Enter password: ")
                val result = userManager.login(email, password)
                println(result)

                if (result.startsWith("Login successful")) {
                    while (true) {
                        println("\n-- Account Options --")
                        println("1. Change Password")
                        println("2. Delete Account")
                        println("3. Logout")
                        print("Select: ")

                        when (readLine()?.trim()) {
                            "1" -> {
                                val current = readPasswordFromConsole("Enter current password: ")
                                val newPass = readPasswordFromConsole("Enter new password: ")
                                println(userManager.changePassword(email, current, newPass))
                            }
                            "2" -> {
                                print("Are you sure? (yes/no): ")
                                if (readLine()?.trim().equals("yes", ignoreCase = true)) {
                                    val confirmPassword = readPasswordFromConsole("Confirm password: ")
                                    println(userManager.deleteAccount(email, confirmPassword))
                                    break
                                }
                            }
                            "3" -> break
                            else -> println("Invalid option")
                        }
                    }
                }
            }

            "3" -> {
                print("Enter email: ")
                val email = readLine()?.trim() ?: ""
                println(userManager.recoverPassword(email))
            }

            "4" -> {
                println("\n--- LOG ---")
                println(userManager.getLogs())
            }

            "5" -> {
                println("Goodbye!")
                return
            }

            else -> println("Invalid option")
        }
    }
}

fun readPasswordFromConsole(prompt: String): String {
    val console = System.console()
    return if (console != null) {
        String(console.readPassword(prompt))
    } else {
        print(prompt)
        readLine()?.trim() ?: ""
    }
}

class SecureConsoleUserManager(private val fileName: String, private val logFileName: String) {
    private val users = mutableMapOf<String, Triple<String, String, Int>>()
    private val maxAttempts = 3
    private val minPasswordLength = 8
    private val pbkdf2Iterations = 310000
    private val pbkdf2KeyLength = 256

    init {
        loadUsers()
    }

    fun register(email: String, password: String): String {
        if (!isValidEmail(email)) return "Invalid email format."
        if (users.containsKey(email)) return "User already exists."

        val passwordError = validatePasswordStrength(password)
        if (passwordError != null) return passwordError

        val salt = generateSalt()
        val hash = hash(password, salt)
        users[email] = Triple(salt, hash, 0)
        saveUsers()
        log("Registered: $email")
        return "Registration successful."
    }

    fun login(email: String, password: String): String {
        if (!isValidEmail(email)) return "Invalid email format."

        val user = users[email] ?: return "No account found."
        val (salt, correctHash, attempts) = user

        if (attempts >= maxAttempts) {
            log("Locked out attempt: $email")
            return "Account locked due to too many failed attempts."
        }

        val inputHash = hash(password, salt)

        return if (inputHash == correctHash) {
            users[email] = Triple(salt, correctHash, 0)
            saveUsers()
            log("Login success: $email")
            "Login successful. Welcome, $email!"
        } else {
            val newAttempts = attempts + 1
            users[email] = Triple(salt, correctHash, newAttempts)
            saveUsers()
            log("Login failed: $email")
            if (newAttempts >= maxAttempts) {
                "Account locked due to too many failed attempts."
            } else {
                "Incorrect password. Attempts left: ${maxAttempts - newAttempts}"
            }
        }
    }

    fun changePassword(email: String, currentPassword: String, newPassword: String): String {
        val user = users[email] ?: return "User not found."
        val (salt, correctHash, attempts) = user

        if (attempts >= maxAttempts) return "Account locked. Cannot change password."

        val currentHash = hash(currentPassword, salt)
        if (currentHash != correctHash) return "Current password is incorrect."

        val passwordError = validatePasswordStrength(newPassword)
        if (passwordError != null) return passwordError

        if (currentPassword == newPassword) return "New password must differ from current password."

        val newSalt = generateSalt()
        val newHash = hash(newPassword, newSalt)
        users[email] = Triple(newSalt, newHash, 0)
        saveUsers()
        log("Password changed: $email")
        return "Password changed successfully."
    }

    fun deleteAccount(email: String, password: String): String {
        val user = users[email] ?: return "User not found."
        val (salt, correctHash, _) = user
        val inputHash = hash(password, salt)

        return if (inputHash == correctHash) {
            users.remove(email)
            saveUsers()
            log("Account deleted: $email")
            "Account deleted successfully."
        } else {
            "Incorrect password. Account not deleted."
        }
    }

    fun recoverPassword(email: String): String {
        if (!isValidEmail(email)) return "Invalid email format."
        if (!users.containsKey(email)) return "No account found with this email."

        val token = generateResetToken()
        log("Recovery token generated for: $email | Token: $token")
        return "Recovery token (check logs in production this would be emailed): $token"
    }

    fun getLogs(): String {
        val file = File(logFileName)
        return if (file.exists()) file.readText() else "No logs yet."
    }

    private fun validatePasswordStrength(password: String): String? {
        if (password.length < minPasswordLength) return "Password must be at least $minPasswordLength characters."
        if (!password.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter."
        if (!password.any { it.isLowerCase() }) return "Password must contain at least one lowercase letter."
        if (!password.any { it.isDigit() }) return "Password must contain at least one digit."
        if (!password.any { !it.isLetterOrDigit() }) return "Password must contain at least one special character."
        return null
    }

    private fun saveUsers() {
        val tempFile = File("$fileName.tmp")
        tempFile.printWriter().use { out ->
            users.forEach { (email, triple) ->
                val (salt, hash, attempts) = triple
                out.println("$email:$salt:$hash:$attempts")
            }
        }
        tempFile.renameTo(File(fileName))
    }

    private fun loadUsers() {
        val file = File(fileName)
        if (!file.exists()) return
        file.forEachLine { line ->
            val parts = line.split(":")
            if (parts.size == 4) {
                val attempts = parts[3].toIntOrNull() ?: 0
                users[parts[0]] = Triple(parts[1], parts[2], attempts)
            }
        }
    }

    private fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            pbkdf2Iterations,
            pbkdf2KeyLength
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(hash)
    }

    private fun generateSalt(length: Int = 16): String {
        val bytes = ByteArray(length).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun generateResetToken(): String {
        val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun isValidEmail(email: String): Boolean {
        return Regex("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$").matches(email)
    }

    private fun log(message: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        File(logFileName).appendText("[$timestamp] $message\n")
    }
}
