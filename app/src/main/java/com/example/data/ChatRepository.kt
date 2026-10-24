package com.example.data

import android.content.Context
import com.example.ai.GeminiClient
import com.example.ai.GeminiResult
import com.example.data.local.HabibDatabase
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val geminiClient: GeminiClient
) {
    private val db = HabibDatabase.getInstance(context)
    private val chatDao = db.chatDao()

    fun getSessionsForUser(userId: String): Flow<List<ChatSessionEntity>> {
        return chatDao.observeSessionsForUser(userId)
    }

    fun getMessagesForSession(sessionId: String, userId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.observeMessagesForSession(sessionId, userId)
    }

    suspend fun createNewSession(userId: String, title: String = "নতুন কথোপকথন"): ChatSessionEntity = withContext(Dispatchers.IO) {
        val session = ChatSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        session
    }

    suspend fun deleteSession(sessionId: String, userId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId, userId)
        chatDao.deleteSession(sessionId, userId)
    }

    suspend fun deleteAllUserSessions(userId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteAllMessagesForUser(userId)
        chatDao.deleteAllSessionsForUser(userId)
    }

    suspend fun sendMessage(
        sessionId: String,
        userId: String,
        prompt: String,
        imageBase64: String? = null,
        userProfile: UserEntity? = null,
        onChunk: (String) -> Unit
    ): GeminiResult = withContext(Dispatchers.IO) {
        val userMsgId = UUID.randomUUID().toString()
        val userMsg = ChatMessageEntity(
            messageId = userMsgId,
            sessionId = sessionId,
            userId = userId,
            sender = "user",
            text = prompt,
            imageBase64 = imageBase64,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMsg)

        // Update session timestamp and title if first message
        val currentSession = chatDao.getSession(sessionId, userId)
        if (currentSession != null) {
            val title = if (currentSession.title == "নতুন কথোপকথন" || currentSession.title.isBlank()) {
                val candidate = prompt.take(30).trim()
                if (candidate.isNotEmpty()) candidate else "কথোপকথন"
            } else {
                currentSession.title
            }
            chatDao.updateSession(currentSession.copy(title = title, updatedAt = System.currentTimeMillis()))
        }

        // Create Assistant placeholder message
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessageEntity(
            messageId = assistantMsgId,
            sessionId = sessionId,
            userId = userId,
            sender = "assistant",
            text = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )
        chatDao.insertMessage(assistantPlaceholder)

        // Fetch context history for this session
        val fullHistory = chatDao.getMessagesForSession(sessionId, userId)
            .filter { it.messageId != assistantMsgId }

        val accumulatedText = StringBuilder()
        val result = geminiClient.streamGenerateContent(
            history = fullHistory,
            newPrompt = prompt,
            imageBase64 = imageBase64,
            userProfile = userProfile,
            onChunk = { chunk ->
                accumulatedText.append(chunk)
                onChunk(chunk)
            }
        )

        when (result) {
            is GeminiResult.Success -> {
                chatDao.updateMessageContent(assistantMsgId, result.text, isStreaming = false)
            }
            is GeminiResult.Failure -> {
                val errorText = result.bengaliMessage
                chatDao.updateMessageContent(assistantMsgId, errorText, isStreaming = false)
            }
        }

        result
    }

    suspend fun retryMessage(
        sessionId: String,
        userId: String,
        failedMessageId: String,
        userProfile: UserEntity?,
        onChunk: (String) -> Unit
    ): GeminiResult = withContext(Dispatchers.IO) {
        val messages = chatDao.getMessagesForSession(sessionId, userId)
        val failedIndex = messages.indexOfFirst { it.messageId == failedMessageId }
        val promptMsg = if (failedIndex > 0) messages[failedIndex - 1] else null

        if (promptMsg == null) {
            return@withContext GeminiResult.Failure("পূর্ববর্তী প্রশ্নটি খুঁজে পাওয়া যায়নি", "No prompt found")
        }

        // Reset assistant message to streaming
        chatDao.updateMessageContent(failedMessageId, "", isStreaming = true)

        val history = messages.take(failedIndex - 1)
        val accumulated = StringBuilder()

        val result = geminiClient.streamGenerateContent(
            history = history,
            newPrompt = promptMsg.text,
            imageBase64 = promptMsg.imageBase64,
            userProfile = userProfile,
            onChunk = { chunk ->
                accumulated.append(chunk)
                onChunk(chunk)
            }
        )

        when (result) {
            is GeminiResult.Success -> {
                chatDao.updateMessageContent(failedMessageId, result.text, isStreaming = false)
            }
            is GeminiResult.Failure -> {
                chatDao.updateMessageContent(failedMessageId, result.bengaliMessage, isStreaming = false)
            }
        }

        result
    }
}
