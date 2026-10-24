package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeSessionsForUser(userId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId AND userId = :userId LIMIT 1")
    suspend fun getSession(sessionId: String, userId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun deleteSession(sessionId: String, userId: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND userId = :userId ORDER BY timestamp ASC")
    fun observeMessagesForSession(sessionId: String, userId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND userId = :userId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String, userId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET text = :text, isStreaming = :isStreaming WHERE messageId = :messageId")
    suspend fun updateMessageContent(messageId: String, text: String, isStreaming: Boolean)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun deleteMessagesForSession(sessionId: String, userId: String)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun deleteAllMessagesForUser(userId: String)

    @Query("DELETE FROM chat_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: String)
}
