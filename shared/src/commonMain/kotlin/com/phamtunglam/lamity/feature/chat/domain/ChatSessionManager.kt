package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.core.platform.AppDirs
import com.phamtunglam.lamity.core.platform.epochMillis
import com.phamtunglam.lamity.core.platform.newId
import com.phamtunglam.lamity.core.tools.ToolContext
import com.phamtunglam.lamity.core.tools.ToolDispatcher
import com.phamtunglam.lamity.core.tools.ToolEvent
import com.phamtunglam.lamity.core.tools.ToolIds
import com.phamtunglam.lamity.core.tools.ToolRegistry
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.studio.domain.Agent
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.domain.Skill
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.llm.ConversationSetup
import com.phamtunglam.lamity.llm.EngineSetup
import com.phamtunglam.lamity.llm.EngineState
import com.phamtunglam.lamity.llm.GenEvent
import com.phamtunglam.lamity.llm.ModelRuntime
import com.phamtunglam.lamity.llm.NativeLlmBridge
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ChatSessionState(
    val conversationId: String? = null,
    val agentId: String? = null,
    val modelId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamingText: String = "",
    val streamingThought: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Idle,
    val error: String? = null,
)

/**
 * Orchestrates chats: sessions, streaming, tool events, history persistence.
 * Lives for the whole process so chat state survives navigation; ViewModels
 * are thin observers of [state].
 */
class ChatSessionManager(
    private val scope: CoroutineScope,
    private val runtime: ModelRuntime,
    private val conversations: ConversationsRepository,
    private val agents: AgentsRepository,
    private val skills: SkillsRepository,
    private val models: ModelsRepository,
    private val settings: SettingsRepository,
    private val registry: ToolRegistry,
    private val toolContext: ToolContext,
    private val dispatcher: ToolDispatcher,
    private val downloads: ModelDownloadManager,
    private val dirs: AppDirs,
    llmBridge: NativeLlmBridge,
) {
    private val log = Logger.withTag("ChatSessionManager")

    private val _state = MutableStateFlow(ChatSessionState())
    val state: StateFlow<ChatSessionState> = _state.asStateFlow()

    private var sessionHandle: String? = null
    private var sessionSignature: String? = null
    private var generateJob: Job? = null

    init {
        // Model tool calls from the native runtimes land in the shared dispatcher.
        llmBridge.toolExecutor = dispatcher

        scope.launch {
            settings.awaitLoaded()
            models.awaitLoaded()
            agents.awaitLoaded()
            val s = settings.value
            _state.update {
                it.copy(
                    agentId = s.lastAgentId.takeIf { id -> agents.byId(id) != null },
                    modelId = (s.lastModelId.takeIf { id -> models.byId(id) != null }
                        ?: models.models.value.firstOrNull()?.id),
                )
            }
        }
        scope.launch {
            runtime.engineState.collect { es -> _state.update { it.copy(engine = es) } }
        }
        scope.launch {
            dispatcher.events.collect { onToolEvent(it) }
        }
    }

    // ----------------------------------------------------------- selection

    fun selectModel(modelId: String) {
        if (modelId == _state.value.modelId) return
        stopGeneration()
        _state.update { it.copy(modelId = modelId, error = null) }
        persistSelection()
    }

    fun selectAgent(agentId: String?) {
        if (agentId == _state.value.agentId) return
        stopGeneration()
        _state.update { it.copy(agentId = agentId, error = null) }
        persistSelection()
    }

    private fun persistSelection() {
        val s = _state.value
        scope.launch { settings.setLastSelection(s.modelId, s.agentId) }
    }

    // ----------------------------------------------------------- lifecycle

    fun newChat() {
        stopGeneration()
        closeSession()
        _state.update {
            it.copy(
                conversationId = null,
                messages = emptyList(),
                streamingText = "",
                streamingThought = "",
                error = null,
            )
        }
    }

    fun openConversation(conversationId: String) {
        val conversation = conversations.byId(conversationId) ?: return
        stopGeneration()
        closeSession()
        scope.launch {
            val messages = conversations.loadMessages(conversationId)
            _state.update {
                it.copy(
                    conversationId = conversation.id,
                    agentId = conversation.agentId.takeIf { id -> agents.byId(id) != null },
                    modelId = models.byId(conversation.modelId)?.id ?: it.modelId,
                    messages = messages,
                    streamingText = "",
                    streamingThought = "",
                    error = null,
                )
            }
            persistSelection()
        }
    }

    fun onConversationDeleted(conversationId: String) {
        if (_state.value.conversationId == conversationId) newChat()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    // ----------------------------------------------------------- generation

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isGenerating) return
        val model = models.byId(_state.value.modelId)
        if (model == null) {
            _state.update { it.copy(error = "Select a model first") }
            return
        }
        if (!downloads.isDownloaded(model)) {
            _state.update { it.copy(error = "Model is not downloaded yet") }
            return
        }

        generateJob = scope.launch {
            _state.update {
                it.copy(isGenerating = true, error = null, streamingText = "", streamingThought = "")
            }
            try {
                runGeneration(model, trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log.e(e) { "generation failed" }
                _state.update { it.copy(error = e.message ?: "Generation failed") }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun stopGeneration() {
        generateJob?.cancel()
        generateJob = null
    }

    private suspend fun runGeneration(model: LlmModel, text: String) {
        val agent = agents.byId(_state.value.agentId)

        val engineKey = "${model.id}|${model.config.backend.name}|${model.config.maxTokens}"
        val engineSetup = EngineSetup(
            modelPath = downloads.modelPath(model),
            backend = model.config.backend.name.lowercase(),
            maxTokens = model.config.maxTokens,
            cacheDir = dirs.cacheDir,
        )
        runtime.ensureEngine(model.id, engineKey, engineSetup).getOrElse {
            _state.update { s -> s.copy(error = it.message ?: "Could not load model") }
            return
        }

        // Conversation row (history) exists before the first token.
        val conversationId = _state.value.conversationId ?: run {
            val created = conversations.create(agent?.id, model.id)
            _state.update { it.copy(conversationId = created.id) }
            created.id
        }
        conversations.ensureTitle(conversationId, text)

        ensureSession(model, agent, conversationId).getOrElse {
            _state.update { s -> s.copy(error = it.message ?: "Could not start conversation") }
            return
        }

        val userMessage = ChatMessage(
            id = newId(),
            conversationId = conversationId,
            role = MessageRole.USER,
            content = text,
            createdAt = epochMillis(),
        )
        conversations.appendMessage(userMessage)
        _state.update { it.copy(messages = it.messages + userMessage) }

        val startedAt = epochMillis()
        var firstTokenAt = 0L
        var charCount = 0
        var completed = false

        try {
            runtime.generate(sessionHandle!!, text).collect { event ->
                when (event) {
                    is GenEvent.Chunk -> {
                        if (firstTokenAt == 0L) firstTokenAt = epochMillis()
                        charCount += event.text.length
                        _state.update { it.copy(streamingText = it.streamingText + event.text) }
                    }
                    is GenEvent.Thought -> {
                        if (firstTokenAt == 0L) firstTokenAt = epochMillis()
                        charCount += event.text.length
                        _state.update { it.copy(streamingThought = it.streamingThought + event.text) }
                    }
                    is GenEvent.Done -> {
                        completed = true
                        finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                    }
                    is GenEvent.Error -> {
                        completed = true
                        finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                        _state.update { it.copy(error = event.message) }
                    }
                }
            }
        } catch (e: CancellationException) {
            // Stop pressed: keep whatever streamed so far. NonCancellable so the
            // persistence writes still run inside the cancelled coroutine.
            if (!completed) {
                withContext(NonCancellable) {
                    finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                    conversations.touch(conversationId, agent?.id, model.id)
                }
            }
            throw e
        }
        conversations.touch(conversationId, agent?.id, model.id)
    }

    private suspend fun finishAssistantMessage(
        conversationId: String,
        startedAt: Long,
        firstTokenAt: Long,
        charCount: Int,
    ) {
        val s = _state.value
        if (s.streamingText.isBlank() && s.streamingThought.isBlank()) {
            _state.update { it.copy(streamingText = "", streamingThought = "") }
            return
        }
        val now = epochMillis()
        val streamMillis = (now - (if (firstTokenAt > 0) firstTokenAt else startedAt)).coerceAtLeast(1)
        val tokensPerSec = (charCount / 4.0) / (streamMillis / 1000.0)
        val message = ChatMessage(
            id = newId(),
            conversationId = conversationId,
            role = MessageRole.ASSISTANT,
            content = s.streamingText.trim(),
            thought = s.streamingThought.trim(),
            genMillis = now - startedAt,
            tokensPerSec = tokensPerSec,
            createdAt = now,
        )
        conversations.appendMessage(message)
        _state.update {
            it.copy(messages = it.messages + message, streamingText = "", streamingThought = "")
        }
    }

    // ----------------------------------------------------------- session

    private suspend fun ensureSession(
        model: LlmModel,
        agent: Agent?,
        conversationId: String,
    ): Result<Unit> {
        val skillsForAgent = effectiveSkills(agent)
        val toolIds = effectiveToolIds(agent, skillsForAgent)
        val signature = sessionSignatureOf(model, agent, conversationId, toolIds, skillsForAgent)
        if (sessionHandle != null && sessionSignature == signature) return Result.success(Unit)

        closeSession()
        // load_skill resolves against the skills attached to this session
        toolContext.activeSkills = { skillsForAgent }

        val history = _state.value.messages.filter { it.role != MessageRole.TOOL }
        val setup = ConversationSetup(
            systemPrompt = buildSystemPrompt(agent, skillsForAgent),
            historyJson = historyJson(history),
            toolIds = toolIds,
            toolSpecsJson = registry.specsJsonFor(toolIds),
            topK = model.config.topK,
            topP = model.config.topP,
            temperature = model.config.temperature,
        )
        return runtime.createConversation(setup).map { handle ->
            sessionHandle = handle
            sessionSignature = signature
        }
    }

    private fun closeSession() {
        sessionHandle?.let { runtime.closeConversation(it) }
        sessionHandle = null
        sessionSignature = null
    }

    private fun effectiveSkills(agent: Agent?): List<Skill> =
        agent?.skillIds.orEmpty()
            .mapNotNull { skills.byId(it) }
            .filter { it.enabled }

    private fun effectiveToolIds(agent: Agent?, skillsForAgent: List<Skill>): List<String> {
        val base = agent?.toolIds
            ?: registry.userSelectable.map { it.id } // no agent: all globally enabled tools
        val enabled = base.filter { id ->
            settings.isToolEnabled(id) && registry.byId(id) != null && id != ToolIds.LOAD_SKILL
        }
        return if (skillsForAgent.isNotEmpty()) enabled + ToolIds.LOAD_SKILL else enabled
    }

    private fun sessionSignatureOf(
        model: LlmModel,
        agent: Agent?,
        conversationId: String,
        toolIds: List<String>,
        skillsForAgent: List<Skill>,
    ): String = buildString {
        append(conversationId)
        append('|').append(model.id)
        append('|').append(model.config.backend).append(model.config.maxTokens)
        append('|').append(model.config.topK).append(model.config.topP).append(model.config.temperature)
        append('|').append(agent?.id).append(':').append(agent?.updatedAt)
        append('|').append(toolIds.sorted().joinToString(","))
        append('|').append(skillsForAgent.joinToString(",") { "${it.id}:${it.updatedAt}" })
    }

    private fun buildSystemPrompt(agent: Agent?, skillsForAgent: List<Skill>): String? {
        val parts = mutableListOf<String>()
        agent?.systemPrompt?.takeIf { it.isNotBlank() }?.let { parts += it.trim() }
        if (skillsForAgent.isNotEmpty()) {
            parts += buildString {
                appendLine("# Skills")
                appendLine(
                    "You have the following optional skills. Before applying a skill, " +
                        "call the load_skill tool with its exact name to get its full instructions."
                )
                for (skill in skillsForAgent) {
                    appendLine("- ${skill.name}: ${skill.description.ifBlank { "(no description)" }}")
                }
            }.trim()
        }
        return parts.joinToString("\n\n").ifBlank { null }
    }

    private fun historyJson(history: List<ChatMessage>): String {
        val arr = buildJsonArray {
            for (m in history) {
                if (m.content.isBlank()) continue
                add(buildJsonObject {
                    put("role", if (m.role == MessageRole.USER) "user" else "model")
                    put("text", m.content)
                })
            }
        }
        return arr.toString()
    }

    // ----------------------------------------------------------- tool events

    private suspend fun onToolEvent(event: ToolEvent) {
        val s = _state.value
        val conversationId = s.conversationId ?: return
        if (!s.isGenerating) return
        val message = ChatMessage(
            id = event.id,
            conversationId = conversationId,
            role = MessageRole.TOOL,
            toolName = event.toolId,
            toolArgs = event.argsJson,
            toolResult = event.resultJson,
            createdAt = event.atMillis,
        )
        conversations.appendMessage(message)
        _state.update { it.copy(messages = it.messages + message) }
    }
}
