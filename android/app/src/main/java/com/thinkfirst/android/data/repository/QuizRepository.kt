package com.thinkfirst.android.data.repository

import android.util.Log
import com.google.gson.Gson
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.local.dao.QuizAttemptDao
import com.thinkfirst.android.data.local.entity.QuizAttemptEntity
import com.thinkfirst.android.data.model.Quiz
import com.thinkfirst.android.data.model.QuizResult
import com.thinkfirst.android.data.model.QuizSubmission
import com.thinkfirst.android.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for quiz operations with offline support
 */
@Singleton
class QuizRepository @Inject constructor(
    private val api: ThinkFirstApi,
    private val quizAttemptDao: QuizAttemptDao,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) {
    
    private val TAG = "QuizRepository"

    /**
     * Get quiz by ID from API
     */
    suspend fun getQuiz(quizId: Long): Result<Quiz> {
        return try {
            val quiz = api.getQuiz(quizId)
            Result.success(quiz)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get quiz $quizId", e)
            Result.failure(e)
        }
    }

    /**
     * Submit a quiz (online or offline)
     */
    suspend fun submitQuiz(
        quizId: Long,
        childId: Long,
        answers: Map<Long, String>,
        timeSpentSeconds: Int? = null
    ): Result<QuizResult> {
        return try {
            if (networkMonitor.isCurrentlyConnected()) {
                // Online: Submit to API
                val submission = QuizSubmission(
                    quizId = quizId,
                    childId = childId,
                    answers = answers,
                    timeSpentSeconds = timeSpentSeconds
                )
                android.util.Log.d(TAG, "Submitting quiz to API - submission: $submission")
                val result = api.submitQuiz(submission)
                
                // Cache the attempt
                cacheAttempt(quizId, childId, result, answers, isSynced = true)
                
                Result.success(result)
            } else {
                // Offline: Cache for later sync
                val offlineResult = QuizResult(
                    score = 0, // Will be calculated on sync
                    passed = false,
                    totalQuestions = answers.size,
                    correctAnswers = 0,
                    questionResults = emptyList(),
                    feedbackMessage = "Quiz saved offline. Will be submitted when online."
                )
                
                cacheAttempt(quizId, childId, offlineResult, answers, isSynced = false)
                
                Result.success(offlineResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit quiz", e)
            
            // If online submission failed, cache offline
            if (networkMonitor.isCurrentlyConnected()) {
                val offlineResult = QuizResult(
                    score = 0,
                    passed = false,
                    totalQuestions = answers.size,
                    correctAnswers = 0,
                    questionResults = emptyList(),
                    feedbackMessage = "Quiz saved offline. Will be submitted when online."
                )
                cacheAttempt(quizId, childId, offlineResult, answers, isSynced = false)
            }
            
            Result.failure(e)
        }
    }
    
    /**
     * Get quiz attempts for a child (from cache)
     */
    fun getAttempts(childId: Long): Flow<List<QuizAttempt>> {
        return quizAttemptDao.getAttemptsByChild(childId).map { entities ->
            entities.map { entity ->
                QuizAttempt(
                    id = entity.id,
                    quizId = entity.quizId,
                    score = entity.score,
                    passed = entity.passed,
                    timestamp = entity.timestamp,
                    isSynced = entity.isSynced
                )
            }
        }
    }
    
    /**
     * Get average score for a child
     */
    suspend fun getAverageScore(childId: Long): Double {
        return quizAttemptDao.getAverageScore(childId) ?: 0.0
    }
    
    /**
     * Cache a quiz attempt
     */
    private suspend fun cacheAttempt(
        quizId: Long,
        childId: Long,
        result: QuizResult,
        answers: Map<Long, String>,
        isSynced: Boolean
    ) {
        try {
            val answersJson = gson.toJson(answers)
            
            val entity = QuizAttemptEntity(
                quizId = quizId,
                childId = childId,
                score = result.score,
                passed = result.passed,
                answers = answersJson,
                timestamp = System.currentTimeMillis(),
                isSynced = isSynced
            )
            
            quizAttemptDao.insertAttempt(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache attempt", e)
        }
    }
}

/**
 * Domain model for quiz attempt
 */
data class QuizAttempt(
    val id: Long,
    val quizId: Long,
    val score: Int,
    val passed: Boolean,
    val timestamp: Long,
    val isSynced: Boolean
)

