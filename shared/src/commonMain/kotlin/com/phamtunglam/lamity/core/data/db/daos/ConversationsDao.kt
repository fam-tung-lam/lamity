package com.phamtunglam.lamity.core.data.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.phamtunglam.lamity.core.data.db.entities.ConversationEntity
import com.phamtunglam.lamity.core.data.db.entities.MessageEntity
import com.phamtunglam.lamity.core.data.db.relations.ConversationWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationsDao {
    @Query("SELECT * FROM conversations ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun byId(id: String): ConversationEntity?

    /** A conversation with its full message log (one-to-many via [ConversationWithMessages]). */
    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun withMessages(id: String): ConversationWithMessages?

    @Upsert
    suspend fun upsert(entity: ConversationEntity)

    // Messages are cascade-deleted via the FK on MessageEntity.conversationId.
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)
}
