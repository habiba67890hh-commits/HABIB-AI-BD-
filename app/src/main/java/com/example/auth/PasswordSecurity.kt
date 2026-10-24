package com.example.auth

import android.util.Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecurity {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPassword(password: CharArray, salt: ByteArray): String {
        return try {
            val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = skf.generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback to SHA-256 if PBKDF2 unavailable
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            val hash = digest.digest(String(password).toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hash, Base64.NO_WRAP)
        }
    }

    fun verifyPassword(password: String, saltBase64: String, expectedHash: String): Boolean {
        val salt = try {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            return false
        }
        val computedHash = hashPassword(password.toCharArray(), salt)
        return computedHash == expectedHash
    }
}
