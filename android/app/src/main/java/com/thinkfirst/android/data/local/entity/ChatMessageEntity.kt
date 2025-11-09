package com.thinkfirst.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room entity for caching chat messages offline
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val childId: Long,
    val query: String,
    val response: String,
    val responseLevel: String, // FULL_ANSWER, PARTIAL_HINT, GUIDED_QUESTIONS
    val timestamp: Long,
    val isSynced: Boolean = false,
    val quizId: Long? = null,
    val quizQuestions: String? = null // JSON string of quiz questions
)

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromQuestionList(value: List<QuizQuestionEntity>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toQuestionList(value: String?): List<QuizQuestionEntity>? {
        return value?.let {
            val type = object : TypeToken<List<QuizQuestionEntity>>() {}.type
            gson.fromJson(it, type)
        }
    }
}

/**
 * Quiz question data for offline storage
 */
data class QuizQuestionEntity(
    val id: Long,
    val questionText: String,
    val options: List<String>?,
    val correctAnswer: String?,
    val explanation: String?
)

