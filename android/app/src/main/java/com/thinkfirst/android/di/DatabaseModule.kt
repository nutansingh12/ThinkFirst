package com.thinkfirst.android.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.thinkfirst.android.data.local.ThinkFirstDatabase
import com.thinkfirst.android.data.local.dao.ChatMessageDao
import com.thinkfirst.android.data.local.dao.QuizAttemptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ThinkFirstDatabase {
        return Room.databaseBuilder(
            context,
            ThinkFirstDatabase::class.java,
            ThinkFirstDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development only
            .build()
    }
    
    @Provides
    @Singleton
    fun provideChatMessageDao(database: ThinkFirstDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
    
    @Provides
    @Singleton
    fun provideQuizAttemptDao(database: ThinkFirstDatabase): QuizAttemptDao {
        return database.quizAttemptDao()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

