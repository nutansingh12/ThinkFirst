package com.thinkfirst.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thinkfirst.android.data.local.dao.ChatMessageDao
import com.thinkfirst.android.data.local.dao.QuizAttemptDao
import com.thinkfirst.android.data.local.entity.ChatMessageEntity
import com.thinkfirst.android.data.local.entity.Converters
import com.thinkfirst.android.data.local.entity.QuizAttemptEntity

/**
 * Room database for offline caching
 */
@Database(
    entities = [
        ChatMessageEntity::class,
        QuizAttemptEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ThinkFirstDatabase : RoomDatabase() {
    
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    
    companion object {
        const val DATABASE_NAME = "thinkfirst_db"
    }
}

