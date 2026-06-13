package com.phamtunglam.lamity.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.phamtunglam.lamity.db.entities.ConversationEntity
import com.phamtunglam.lamity.db.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationsDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun byId(id: String): ConversationEntity?

    @Upsert
    suspend fun upsert(entity: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun messagesFor(conversationId: String): List<MessageEntity>

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesFor(conversationId: String)
}
