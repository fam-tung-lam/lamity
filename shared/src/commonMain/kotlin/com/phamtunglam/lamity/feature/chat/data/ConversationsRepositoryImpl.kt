package com.phamtunglam.lamity.feature.chat.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.data.db.daos.ConversationsDao
import com.phamtunglam.lamity.core.data.db.entities.ConversationEntity
import com.phamtunglam.lamity.core.data.db.entities.MessageEntity
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.chat.domain.ChatMessage
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import com.phamtunglam.lamity.feature.chat.domain.MessageRole
import com.phamtunglam.lamity.llm.model.Role
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Conversations and messages live in Room; the database is the single source of truth. */
class ConversationsRepositoryImpl(private val dao: ConversationsDao, scope: CoroutineScope) : ConversationsRepository {
    private val log = Logger.withTag("ConversationsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val conversations: StateFlow<List<Conversation>> =
        dao
            .observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { e ->
                log.e(e) { "failed to observe conversations" }
                emit(emptyList())
            }.onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): Conversation? = id?.let { i -> conversations.value.firstOrNull { it.id == i } }

    override suspend fun create(): Conversation {
        val now = epochMillis()
        val conversation = Conversation(id = newId(), title = "", createdAt = now, updatedAt = now)
        runCatching { dao.upsert(conversation.toEntity()) }
            .onFailure { log.e(it) { "failed to persist conversation ${conversation.id}" } }
        return conversation
    }

    override suspend fun rename(id: String, title: String) {
        // Read the row from the database so renames never race the flow cache.
        val current = runCatching { dao.byId(id) }.getOrNull() ?: return
        runCatching { dao.upsert(current.copy(title = title.trim())) }
            .onFailure { log.e(it) { "failed to rename conversation $id" } }
    }

    override suspend fun ensureTitle(id: String, candidate: String) {
        val current = runCatching { dao.byId(id) }.getOrNull() ?: return
        if (current.title.isNotBlank()) return
        val title =
            candidate
                .trim()
                .replace('\n', ' ')
                .take(48)
                .ifBlank { "New chat" }
        runCatching { dao.upsert(current.copy(title = title)) }
            .onFailure { log.e(it) { "failed to title conversation $id" } }
    }

    override suspend fun touch(id: String) {
        val current = runCatching { dao.byId(id) }.getOrNull() ?: return
        runCatching { dao.upsert(current.copy(updatedAt = epochMillis())) }
            .onFailure { log.e(it) { "failed to touch conversation $id" } }
    }

    override suspend fun delete(id: String) {
        // Messages cascade-delete via the FK on MessageEntity.conversationId.
        runCatching { dao.delete(id) }
            .onFailure { log.e(it) { "failed to delete conversation $id" } }
    }

    override suspend fun loadMessages(conversationId: String): List<ChatMessage> =
        runCatching {
            dao
                .withMessages(conversationId)
                ?.messages
                ?.sortedBy { it.createdAt }
                ?.map { it.toDomain() }
                .orEmpty()
        }.onFailure { log.e(it) { "failed to load messages for $conversationId" } }
            .getOrDefault(emptyList())

    override suspend fun appendMessage(message: ChatMessage) {
        runCatching { dao.upsertMessage(message.toEntity()) }
            .onFailure { log.e(it) { "failed to persist message ${message.id}" } }
    }
}

private fun ConversationEntity.toDomain() =
    Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Conversation.toEntity() =
    ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun MessageEntity.toDomain() =
    ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role.toMessageRole(),
        content = content,
        thought = thought,
        toolName = toolName,
        toolArgs = toolArgs,
        toolResult = toolResult,
        genMillis = genMillis,
        tokensPerSec = tokensPerSec,
        createdAt = createdAt,
    )

private fun ChatMessage.toEntity() =
    MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.toLlmRole(),
        content = content,
        thought = thought,
        toolName = toolName,
        toolArgs = toolArgs,
        toolResult = toolResult,
        genMillis = genMillis,
        tokensPerSec = tokensPerSec,
        createdAt = createdAt,
    )

/** The persisted [Role] (lamityLlm) maps onto the chat-domain [MessageRole]. */
private fun MessageRole.toLlmRole(): Role =
    when (this) {
        MessageRole.USER -> Role.User
        MessageRole.ASSISTANT -> Role.Model
        MessageRole.TOOL -> Role.Tool
    }

private fun Role.toMessageRole(): MessageRole =
    when (this) {
        Role.User -> MessageRole.USER
        Role.Tool -> MessageRole.TOOL
        Role.Model, Role.System -> MessageRole.ASSISTANT
    }
