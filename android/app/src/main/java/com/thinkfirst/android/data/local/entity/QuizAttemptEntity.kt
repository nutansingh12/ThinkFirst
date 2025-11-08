package com.thinkfirst.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching quiz attempts offline
 */
@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quizId: Long,
    val childId: Long,
    val score: Int,
    val passed: Boolean,
    val answers: String, // JSON string of answers map
    val timestamp: Long,
    val isSynced: Boolean = false
)

