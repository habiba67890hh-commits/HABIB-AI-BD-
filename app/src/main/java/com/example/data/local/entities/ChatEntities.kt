package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_sessions",
    indices = [Index(value = ["userId"])]
)
data class ChatSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"]), Index(value = ["userId"])]
)
data class ChatMessageEntity(
    @PrimaryKey
    val messageId: String,
    val sessionId: String,
    val userId: String,
    val sender: String, // "user", "assistant", "system_error"
    val text: String,
    val imageUri: String? = null,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)
