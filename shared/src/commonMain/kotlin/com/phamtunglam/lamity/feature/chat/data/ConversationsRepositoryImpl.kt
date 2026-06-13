package com.phamtunglam.lamity.feature.chat.data

import com.phamtunglam.lamity.core.platform.epochMillis
import com.phamtunglam.lamity.core.platform.newId
import com.phamtunglam.lamity.db.entities.ConversationEntity
import com.phamtunglam.lamity.db.daos.ConversationsDao
import com.phamtunglam.lamity.db.entities.MessageEntity
import com.phamtunglam.lamity.feature.chat.domain.ChatMessage
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import com.phamtunglam.lamity.feature.chat.domain.MessageRole
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Conversations and messages live in Room; the database is the single source of truth. */
class ConversationsRepositoryImpl(
    private val dao: ConversationsDao,
    scope: CoroutineScope,
) : ConversationsRepository {

    private val log = Logger.withTag("ConversationsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val conversations: StateFlow<List<Conversation>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .catch { e ->
            log.e(e) { "failed to observe conversations" }
            emit(emptyList())
        }
        .onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): Conversation? =
        id?.let { i -> conversations.value.firstOrNull { it.id == i } }

    override suspend fun create(agentId: String?, modelId: String): Conversation {
        val now = epochMillis()
        val conversation = Conversation(
            id = newId(),
            title = "",
            agentId = agentId,
            modelId = modelId,
            createdAt = now,
            updatedAt = now,
        )
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
        val title = candidate.trim().replace('\n', ' ').take(48).ifBlank { "New chat" }
        runCatching { dao.upsert(current.copy(title = title)) }
            .onFailure { log.e(it) { "failed to title conversation $id" } }
    }

    override suspend fun touch(id: String, agentId: String?, modelId: String) {
        val current = runCatching { dao.byId(id) }.getOrNull() ?: return
        runCatching {
            dao.upsert(current.copy(agentId = agentId, modelId = modelId, updatedAt = epochMillis()))
        }.onFailure { log.e(it) { "failed to touch conversation $id" } }
    }

    override suspend fun delete(id: String) {
        runCatching {
            dao.delete(id)
            dao.deleteMessagesFor(id)
        }.onFailure { log.e(it) { "failed to delete conversation $id" } }
    }

    override suspend fun loadMessages(conversationId: String): List<ChatMessage> =
        runCatching { dao.messagesFor(conversationId).map { it.toDomain() } }
            .onFailure { log.e(it) { "failed to load messages for $conversationId" } }
            .getOrDefault(emptyList())

    override suspend fun appendMessage(message: ChatMessage) {
        runCatching { dao.upsertMessage(message.toEntity()) }
            .onFailure { log.e(it) { "failed to persist message ${message.id}" } }
    }
}

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    agentId = agentId,
    modelId = modelId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    agentId = agentId,
    modelId = modelId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    conversationId = conversationId,
    role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.ASSISTANT),
    content = content,
    thought = thought,
    toolName = toolName,
    toolArgs = toolArgs,
    toolResult = toolResult,
    genMillis = genMillis,
    tokensPerSec = tokensPerSec,
    createdAt = createdAt,
)

private fun ChatMessage.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    thought = thought,
    toolName = toolName,
    toolArgs = toolArgs,
    toolResult = toolResult,
    genMillis = genMillis,
    tokensPerSec = tokensPerSec,
    createdAt = createdAt,
)
