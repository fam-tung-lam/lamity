package com.phamtunglam.lamity.feature.chat.data

import com.phamtunglam.lamity.feature.chat.domain.ChatMessage
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import kotlinx.coroutines.flow.StateFlow

/** Conversation metadata plus per-conversation message logs. */
interface ConversationsRepository {
    val conversations: StateFlow<List<Conversation>>

    /** Completes once persisted conversations have been loaded into [conversations]. */
    suspend fun awaitLoaded()

    fun byId(id: String?): Conversation?

    suspend fun create(): Conversation

    suspend fun rename(id: String, title: String)

    /** Sets the title from the first user message if it has not been named yet. */
    suspend fun ensureTitle(id: String, candidate: String)

    /** Bumps the updatedAt timestamp. */
    suspend fun touch(id: String)

    suspend fun delete(id: String)

    suspend fun loadMessages(conversationId: String): List<ChatMessage>

    suspend fun appendMessage(message: ChatMessage)
}
