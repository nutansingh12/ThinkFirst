package com.thinkfirst.android.data.repository

import android.util.Log
import com.google.gson.Gson
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.local.dao.ChatMessageDao
import com.thinkfirst.android.data.local.entity.ChatMessageEntity
import com.thinkfirst.android.data.local.entity.QuizQuestionEntity
import com.thinkfirst.android.data.model.ChatRequest
import com.thinkfirst.android.data.model.ChatResponse
import com.thinkfirst.android.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for chat operations with offline support
 */
@Singleton
class ChatRepository @Inject constructor(
    private val api: ThinkFirstApi,
    private val chatMessageDao: ChatMessageDao,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) {
    
    private val TAG = "ChatRepository"
    
    /**
     * Send a chat query (online or offline)
     */
    suspend fun sendQuery(
        childId: Long,
        query: String,
        sessionId: Long
    ): Result<ChatResponse> {
        return try {
            if (networkMonitor.isCurrentlyConnected()) {
                // Online: Send to API
                val request = ChatRequest(childId = childId, sessionId = sessionId, query = query)
                val response = api.sendQuery(request)
                
                // Cache the response
                cacheMessage(childId, sessionId, query, response)
                
                Result.success(response)
            } else {
                // Offline: Return cached response or error
                Result.failure(Exception("No internet connection. Please try again when online."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send query", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get chat messages for a child (from cache)
     */
    fun getMessages(childId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesByChild(childId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    query = entity.query,
                    response = entity.response,
                    responseLevel = entity.responseLevel,
                    timestamp = entity.timestamp,
                    quizId = entity.quizId,
                    quizQuestions = entity.quizQuestions?.let { json ->
                        gson.fromJson(json, Array<QuizQuestionEntity>::class.java).toList()
                    }
                )
            }
        }
    }
    
    /**
     * Get chat messages for a session (from cache)
     */
    fun getSessionMessages(sessionId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesBySession(sessionId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    query = entity.query,
                    response = entity.response,
                    responseLevel = entity.responseLevel,
                    timestamp = entity.timestamp,
                    quizId = entity.quizId,
                    quizQuestions = entity.quizQuestions?.let { json ->
                        gson.fromJson(json, Array<QuizQuestionEntity>::class.java).toList()
                    }
                )
            }
        }
    }
    
    /**
     * Cache a chat message
     */
    private suspend fun cacheMessage(
        childId: Long,
        sessionId: Long,
        query: String,
        response: ChatResponse
    ) {
        try {
            val quizQuestionsJson = response.quiz?.questions?.let { questions ->
                val quizQuestions = questions.map { q ->
                    QuizQuestionEntity(
                        id = q.id,
                        questionText = q.questionText,
                        options = q.options,
                        correctAnswer = q.correctAnswer,
                        explanation = q.explanation
                    )
                }
                gson.toJson(quizQuestions)
            }
            
            val entity = ChatMessageEntity(
                sessionId = sessionId,
                childId = childId,
                query = query,
                response = response.response ?: response.message ?: "",
                responseLevel = response.responseLevel ?: response.responseType.name,
                timestamp = System.currentTimeMillis(),
                isSynced = true,
                quizId = response.quiz?.id,
                quizQuestions = quizQuestionsJson
            )
            
            chatMessageDao.insertMessage(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache message", e)
        }
    }
    
    /**
     * Clear all cached messages for a child
     */
    suspend fun clearMessages(childId: Long) {
        chatMessageDao.deleteMessagesByChild(childId)
    }
}

/**
 * Domain model for chat message
 */
data class ChatMessage(
    val id: Long,
    val query: String,
    val response: String,
    val responseLevel: String,
    val timestamp: Long,
    val quizId: Long?,
    val quizQuestions: List<QuizQuestionEntity>?
)

