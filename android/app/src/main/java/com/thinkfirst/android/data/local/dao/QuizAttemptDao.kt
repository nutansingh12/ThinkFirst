package com.thinkfirst.android.data.local.dao

import androidx.room.*
import com.thinkfirst.android.data.local.entity.QuizAttemptEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for quiz attempt operations
 */
@Dao
interface QuizAttemptDao {
    
    @Query("SELECT * FROM quiz_attempts WHERE childId = :childId ORDER BY timestamp DESC")
    fun getAttemptsByChild(childId: Long): Flow<List<QuizAttemptEntity>>
    
    @Query("SELECT * FROM quiz_attempts WHERE quizId = :quizId ORDER BY timestamp DESC")
    fun getAttemptsByQuiz(quizId: Long): Flow<List<QuizAttemptEntity>>
    
    @Query("SELECT * FROM quiz_attempts WHERE isSynced = 0")
    suspend fun getUnsyncedAttempts(): List<QuizAttemptEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempts(attempts: List<QuizAttemptEntity>)
    
    @Update
    suspend fun updateAttempt(attempt: QuizAttemptEntity)
    
    @Query("UPDATE quiz_attempts SET isSynced = 1 WHERE id = :attemptId")
    suspend fun markAsSynced(attemptId: Long)
    
    @Query("DELETE FROM quiz_attempts WHERE childId = :childId")
    suspend fun deleteAttemptsByChild(childId: Long)
    
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE childId = :childId")
    suspend fun getAttemptCount(childId: Long): Int
    
    @Query("SELECT AVG(score) FROM quiz_attempts WHERE childId = :childId")
    suspend fun getAverageScore(childId: Long): Double?
}

