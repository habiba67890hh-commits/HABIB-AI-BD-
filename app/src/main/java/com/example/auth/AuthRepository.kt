package com.example.auth

import android.content.Context
import android.util.Base64
import com.example.data.local.HabibDatabase
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val englishCause: String? = null) : AuthResult<Nothing>()
}

class AuthRepository(private val context: Context) {
    private val db = HabibDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val chatDao = db.chatDao()

    private val prefs = context.getSharedPreferences("habib_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    init {
        val savedUserId = prefs.getString("current_user_id", null)
        if (savedUserId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val user = userDao.getUserById(savedUserId)
                _currentUser.value = user
            }
        }
    }

    suspend fun register(
        email: String,
        password: CharArray,
        preferredName: String
    ): AuthResult<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext AuthResult.Error(
                message = "সঠিক ইমেল ঠিকানা লিখুন (যেমন: user@example.com)",
                englishCause = "Invalid email format"
            )
        }
        if (password.size < 6) {
            return@withContext AuthResult.Error(
                message = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে",
                englishCause = "Password must be at least 6 characters"
            )
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext AuthResult.Error(
                message = "এই ইমেল দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট রয়েছে",
                englishCause = "Account already exists with this email"
            )
        }

        val salt = PasswordSecurity.generateSalt()
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val hash = PasswordSecurity.hashPassword(password, saltBytes)

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            email = trimmedEmail,
            passwordHash = hash,
            salt = salt,
            preferredName = preferredName.trim().ifEmpty { trimmedEmail.substringBefore("@") },
            languagePreference = "bn"
        )

        userDao.insertUser(newUser)
        setCurrentSession(newUser)
        AuthResult.Success(newUser)
    }

    suspend fun login(email: String, password: CharArray): AuthResult<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            return@withContext AuthResult.Error(
                message = "অনুগ্রহ করে ইমেল ঠিকানা লিখুন",
                englishCause = "Email is required"
            )
        }

        val user = userDao.getUserByEmail(trimmedEmail)
        if (user == null) {
            return@withContext AuthResult.Error(
                message = "এই ইমেলের কোনো অ্যাকাউন্ট পাওয়া যায়নি",
                englishCause = "No account found with this email"
            )
        }

        val valid = PasswordSecurity.verifyPassword(String(password), user.salt, user.passwordHash)
        if (!valid) {
            return@withContext AuthResult.Error(
                message = "ভুল পাসওয়ার্ড! অনুগ্রহ করে আবার চেষ্টা করুন",
                englishCause = "Incorrect password"
            )
        }

        setCurrentSession(user)
        AuthResult.Success(user)
    }

    suspend fun resetPassword(email: String, newPassword: CharArray): AuthResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            return@withContext AuthResult.Error("অনুগ্রহ করে ইমেল ঠিকানা লিখুন")
        }
        if (newPassword.size < 6) {
            return@withContext AuthResult.Error("নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে")
        }

        val user = userDao.getUserByEmail(trimmedEmail)
        if (user == null) {
            return@withContext AuthResult.Error("এই ইমেলের কোনো অ্যাকাউন্ট পাওয়া যায়নি")
        }

        val newSalt = PasswordSecurity.generateSalt()
        val saltBytes = Base64.decode(newSalt, Base64.NO_WRAP)
        val newHash = PasswordSecurity.hashPassword(newPassword, saltBytes)

        val updated = user.copy(passwordHash = newHash, salt = newSalt)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = updated
        }
        AuthResult.Success(Unit)
    }

    suspend fun updateProfile(
        preferredName: String,
        educationLevel: String,
        district: String,
        languagePreference: String,
        responseStyle: String,
        speechRate: Float,
        useFemaleVoice: Boolean
    ): AuthResult<UserEntity> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext AuthResult.Error("ব্যবহারকারী লগইন অবস্থায় নেই")

        val updated = user.copy(
            preferredName = preferredName.trim(),
            educationLevel = educationLevel.trim(),
            district = district.trim(),
            languagePreference = languagePreference,
            responseStyle = responseStyle,
            speechRate = speechRate,
            useFemaleVoice = useFemaleVoice
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
        AuthResult.Success(updated)
    }

    suspend fun deleteCurrentAccount(): AuthResult<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext AuthResult.Error("কোনো ব্যবহারকারী সক্রিয় নেই")
        chatDao.deleteAllMessagesForUser(user.id)
        chatDao.deleteAllSessionsForUser(user.id)
        userDao.deleteUser(user.id)
        logout()
        AuthResult.Success(Unit)
    }

    fun logout() {
        prefs.edit().remove("current_user_id").apply()
        _currentUser.value = null
    }

    private fun setCurrentSession(user: UserEntity) {
        prefs.edit().putString("current_user_id", user.id).apply()
        _currentUser.value = user
    }
}
