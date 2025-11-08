package com.thinkfirst.android.data.local.dao

import androidx.room.*
import com.thinkfirst.android.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for chat message operations
 */
@Dao
interface ChatMessageDao {
    
    @Query("SELECT * FROM chat_messages WHERE childId = :childId ORDER BY timestamp DESC")
    fun getMessagesByChild(childId: Long): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<ChatMessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)
    
    @Query("UPDATE chat_messages SET isSynced = 1 WHERE id = :messageId")
    suspend fun markAsSynced(messageId: Long)
    
    @Query("DELETE FROM chat_messages WHERE childId = :childId")
    suspend fun deleteMessagesByChild(childId: Long)
    
    @Query("DELETE FROM chat_messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM chat_messages WHERE childId = :childId")
    suspend fun getMessageCount(childId: Long): Int
}

