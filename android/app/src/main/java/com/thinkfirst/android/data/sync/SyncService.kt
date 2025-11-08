package com.thinkfirst.android.data.sync

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.local.dao.ChatMessageDao
import com.thinkfirst.android.data.local.dao.QuizAttemptDao
import com.thinkfirst.android.data.local.entity.ChatMessageEntity
import com.thinkfirst.android.data.local.entity.QuizAttemptEntity
import com.thinkfirst.android.data.model.QuizSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing offline data with the backend
 */
@Singleton
class SyncService @Inject constructor(
    private val api: ThinkFirstApi,
    private val chatMessageDao: ChatMessageDao,
    private val quizAttemptDao: QuizAttemptDao,
    private val gson: Gson
) {
    
    private val TAG = "SyncService"
    
    /**
     * Sync all unsynced data with the backend
     */
    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync...")
            
            // Sync quiz attempts first (they're more important)
            syncQuizAttempts()
            
            // Then sync chat messages
            syncChatMessages()
            
            Log.d(TAG, "Sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync unsynced quiz attempts
     */
    private suspend fun syncQuizAttempts() {
        val unsyncedAttempts = quizAttemptDao.getUnsyncedAttempts()
        Log.d(TAG, "Syncing ${unsyncedAttempts.size} quiz attempts")
        
        unsyncedAttempts.forEach { attempt ->
            try {
                // Parse answers from JSON
                val answersType = object : TypeToken<Map<Long, String>>() {}.type
                val answers: Map<Long, String> = gson.fromJson(attempt.answers, answersType)
                
                // Submit to backend
                val submission = QuizSubmission(
                    quizId = attempt.quizId,
                    childId = attempt.childId,
                    answers = answers
                )
                
                api.submitQuiz(submission)
                
                // Mark as synced
                quizAttemptDao.markAsSynced(attempt.id)
                Log.d(TAG, "Synced quiz attempt ${attempt.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync quiz attempt ${attempt.id}", e)
                // Continue with next attempt
            }
        }
    }
    
    /**
     * Sync unsynced chat messages
     */
    private suspend fun syncChatMessages() {
        val unsyncedMessages = chatMessageDao.getUnsyncedMessages()
        Log.d(TAG, "Syncing ${unsyncedMessages.size} chat messages")
        
        unsyncedMessages.forEach { message ->
            try {
                // Note: Chat messages are typically created by the backend
                // We only need to mark them as synced if they were created offline
                // For now, we'll just mark them as synced
                chatMessageDao.markAsSynced(message.id)
                Log.d(TAG, "Synced chat message ${message.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync chat message ${message.id}", e)
                // Continue with next message
            }
        }
    }
    
    /**
     * Clean up old cached data (older than 30 days)
     */
    suspend fun cleanupOldData() = withContext(Dispatchers.IO) {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            chatMessageDao.deleteOldMessages(thirtyDaysAgo)
            Log.d(TAG, "Cleaned up old messages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old data", e)
        }
    }
}

