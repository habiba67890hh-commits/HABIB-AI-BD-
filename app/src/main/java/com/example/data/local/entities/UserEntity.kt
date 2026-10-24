package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val preferredName: String = "",
    val educationLevel: String = "",
    val district: String = "",
    val languagePreference: String = "bn", // "bn", "en", "both"
    val responseStyle: String = "caring_intelligent", // "caring_intelligent", "concise", "academic"
    val speechRate: Float = 1.0f,
    val useFemaleVoice: Boolean = true,
    val isCloudSyncEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
