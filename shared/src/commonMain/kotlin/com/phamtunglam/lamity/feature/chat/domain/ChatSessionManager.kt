package com.phamtunglam.lamity.feature.chat.domain

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.core.domain.tools.AppTool
import com.phamtunglam.lamity.core.domain.tools.LoadSkillTool
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.feature.studio.domain.Agent
import com.phamtunglam.lamity.feature.studio.domain.Skill
import com.phamtunglam.lamity.llm.model.Backend
import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.tool.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatSessionState(
    val conversationId: String? = null,
    val agentId: String? = null,
    val modelId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamingText: String = "",
    val streamingThought: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Idle,
    val error: ChatError? = null,
    val notice: ChatNotice? = null,
)

/**
 * Orchestrates chats: sessions, streaming, tool events, history persistence.
 * Lives for the whole process so chat state survives navigation; ViewModels
 * are thin observers of [state].
 */
@Suppress("TooManyFunctions") // Cohesive chat orchestrator; splitting would scatter related logic.
class ChatSessionManager(
    private val scope: CoroutineScope,
    private val runtime: ModelRuntime,
    private val conversations: ConversationsRepository,
    private val agents: AgentsRepository,
    private val skills: SkillsRepository,
    private val models: ModelsRepository,
    private val settings: SettingsRepository,
    /** User-selectable built-in tools (everything except the per-session load_skill). */
    private val selectableTools: List<AppTool>,
    private val downloads: ModelDownloadManager,
    private val dirs: AppDirs,
) {
    private val log = Logger.withTag("ChatSessionManager")

    private val _state = MutableStateFlow(ChatSessionState())
    val state: StateFlow<ChatSessionState> = _state.asStateFlow()

    /** Tool invocations recorded during generation; persisted and shown as TOOL messages. */
    private val toolMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)

    private var sessionHandle: String? = null
    private var sessionSignature: String? = null
    private var generateJob: Job? = null

    init {
        scope.launch {
            settings.awaitLoaded()
            models.awaitLoaded()
            agents.awaitLoaded()
            val s = settings.value
            _state.update {
                it.copy(
                    agentId = s.lastAgentId.takeIf { id -> agents.byId(id) != null },
                    modelId = (
                        s.lastModelId.takeIf { id -> models.byId(id) != null }
                            ?: models.models.value
                                .firstOrNull()
                                ?.id
                    ),
                )
            }
        }
        scope.launch {
            runtime.engineState.collect { es -> _state.update { it.copy(engine = es) } }
        }
        scope.launch {
            toolMessages.collect { message ->
                conversations.appendMessage(message)
                _state.update { it.copy(messages = it.messages + message) }
            }
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

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    // ----------------------------------------------------------- generation

    // Surfaces any generation failure (including arbitrary native LLM errors) to the UI; cancellation is rethrown.
    @Suppress("TooGenericExceptionCaught")
    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isGenerating) return
        val model = models.byId(_state.value.modelId)
        if (model == null) {
            _state.update { it.copy(error = ChatError.Raw("Select a model first")) }
            return
        }
        if (!downloads.isDownloaded(model)) {
            _state.update { it.copy(error = ChatError.Raw("Model is not downloaded yet")) }
            return
        }

        generateJob =
            scope.launch {
                _state.update {
                    it.copy(
                        isGenerating = true,
                        error = null,
                        notice = null,
                        streamingText = "",
                        streamingThought = "",
                    )
                }
                try {
                    runGeneration(model, trimmed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.e(e) { "generation failed" }
                    _state.update { it.copy(error = ChatError.Raw(e.message ?: "Generation failed")) }
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

        val effectiveModel = loadEngineWithFallback(model) ?: return

        // Conversation row (history) exists before the first token.
        val conversationId =
            _state.value.conversationId ?: run {
                val created = conversations.create(agent?.id, effectiveModel.id)
                _state.update { it.copy(conversationId = created.id) }
                created.id
            }
        conversations.ensureTitle(conversationId, text)

        ensureSession(effectiveModel, agent, conversationId).getOrElse {
            _state.update { s -> s.copy(error = ChatError.Raw(it.message ?: "Could not start conversation")) }
            return
        }

        val userMessage =
            ChatMessage(
                id = newId(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                createdAt = epochMillis(),
            )
        conversations.appendMessage(userMessage)
        _state.update { it.copy(messages = it.messages + userMessage) }

        streamAndPersist(effectiveModel, agent, conversationId, text)
    }

    private suspend fun ensureEngineFor(model: LlmModel): Result<Unit> {
        val engineKey = "${model.id}|${model.config.backend.name}|${model.config.maxTokens}"
        val engineConfig =
            EngineConfig(
                modelPath = downloads.modelPath(model),
                backend = if (model.config.backend == LlmBackend.GPU) Backend.Gpu() else Backend.Cpu(),
                maxNumTokens = model.config.maxTokens,
                cacheDir = dirs.cacheDir,
            )
        return runtime.ensureEngine(model.id, engineKey, engineConfig)
    }

    /**
     * Loads the engine for [model], transparently retrying once on the CPU backend when a GPU load
     * fails with a recoverable native error (see [shouldFallBackToCpu] — e.g. the Gemma 4 E-series
     * `llm_litert_compiled_model_executor` failures). A successful CPU fallback is persisted and a
     * [ChatNotice.SWITCHED_TO_CPU] notice is shown. Returns the effective model to use for the
     * conversation, or null after setting [ChatSessionState.error] on failure.
     */
    private suspend fun loadEngineWithFallback(model: LlmModel): LlmModel? {
        val first = ensureEngineFor(model)
        if (first.isSuccess) return model

        val cause = first.exceptionOrNull()
        if (!shouldFallBackToCpu(model.config.backend, cause?.message)) {
            _state.update { it.copy(error = loadError(cause?.message)) }
            return null
        }

        log.w { "GPU load failed for ${model.id}; retrying on CPU: ${cause?.message}" }
        val cpuModel = model.copy(config = model.config.copy(backend = LlmBackend.CPU))
        if (ensureEngineFor(cpuModel).isFailure) {
            _state.update { it.copy(error = ChatError.Known(ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE)) }
            return null
        }
        models.updateConfig(model.id, cpuModel.config)
        _state.update { it.copy(notice = ChatNotice.SWITCHED_TO_CPU, error = null) }
        return cpuModel
    }

    private suspend fun streamAndPersist(
        model: LlmModel,
        agent: Agent?,
        conversationId: String,
        text: String,
    ) {
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
                        handleGenerationError(model, hadOutput = charCount > 0, message = event.message)
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
        val message =
            ChatMessage(
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

    /**
     * Surfaces a generation failure. When a GPU model fails before producing any output with a
     * recoverable native error, the model is transparently switched to CPU (persisted) and a
     * [ChatNotice.SWITCHED_TO_CPU] is shown so the next send runs on CPU; otherwise the error is
     * mapped to a user-facing [ChatError].
     */
    private suspend fun handleGenerationError(model: LlmModel, hadOutput: Boolean, message: String) {
        if (!hadOutput && shouldFallBackToCpu(model.config.backend, message)) {
            log.w { "GPU generation failed for ${model.id}; switching to CPU: $message" }
            models.updateConfig(model.id, model.config.copy(backend = LlmBackend.CPU))
            _state.update { it.copy(notice = ChatNotice.SWITCHED_TO_CPU, error = null) }
            return
        }
        _state.update { it.copy(error = loadError(message)) }
    }

    // ----------------------------------------------------------- session

    private suspend fun ensureSession(model: LlmModel, agent: Agent?, conversationId: String): Result<Unit> {
        val skillsForAgent = effectiveSkills(agent)
        val toolIds = effectiveToolIds(agent, skillsForAgent)
        val signature = sessionSignatureOf(model, agent, conversationId, toolIds, skillsForAgent)
        if (sessionHandle != null && sessionSignature == signature) return Result.success(Unit)

        closeSession()

        val history = _state.value.messages.filter { it.role != MessageRole.TOOL }
        val config =
            ConversationConfig(
                systemMessage = buildSystemPrompt(agent, skillsForAgent)?.let { Message.system(it) },
                initialMessages = historyMessages(history),
                tools = sessionTools(toolIds, skillsForAgent),
                samplerConfig =
                    SamplerConfig(
                        topK = model.config.topK,
                        topP = model.config.topP,
                        temperature = model.config.temperature,
                    ),
            )
        return runtime.createConversation(config).map { handle ->
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
        agent
            ?.skillIds
            .orEmpty()
            .mapNotNull { skills.byId(it) }
            .filter { it.enabled }

    private fun effectiveToolIds(agent: Agent?, skillsForAgent: List<Skill>): List<String> {
        val base =
            agent?.toolIds
                ?: selectableTools.map { it.id } // no agent: all globally enabled tools
        val enabled =
            base.filter { id ->
                settings.isToolEnabled(id) && selectableTools.any { it.id == id } && id != LoadSkillTool.ID
            }
        return if (skillsForAgent.isNotEmpty()) enabled + LoadSkillTool.ID else enabled
    }

    /**
     * Builds the executable tools for this session in [toolIds] order, wiring each one's invocation
     * sink to the chat recorder. load_skill is created here with the session's [skillsForAgent].
     */
    private fun sessionTools(toolIds: List<String>, skillsForAgent: List<Skill>): List<Tool> =
        buildList {
            toolIds.forEach { id ->
                when (id) {
                    LoadSkillTool.ID -> {
                        add(LoadSkillTool(skillsForAgent).apply { onInvoked = ::recordToolInvocation })
                    }

                    else -> {
                        selectableTools.firstOrNull { it.id == id }?.let { tool ->
                            tool.onInvoked = ::recordToolInvocation
                            add(tool)
                        }
                    }
                }
            }
        }

    private fun sessionSignatureOf(
        model: LlmModel,
        agent: Agent?,
        conversationId: String,
        toolIds: List<String>,
        skillsForAgent: List<Skill>,
    ): String =
        buildString {
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
            parts +=
                buildString {
                    appendLine("# Skills")
                    appendLine(
                        "You have the following optional skills. Before applying a skill, " +
                            "call the load_skill tool with its exact name to get its full instructions.",
                    )
                    for (skill in skillsForAgent) {
                        appendLine("- ${skill.name}: ${skill.description.ifBlank { "(no description)" }}")
                    }
                }.trim()
        }
        return parts.joinToString("\n\n").ifBlank { null }
    }

    private fun historyMessages(history: List<ChatMessage>): List<Message> =
        history.mapNotNull { m ->
            when {
                m.content.isBlank() -> null
                m.role == MessageRole.USER -> Message.user(m.content)
                else -> Message.model(m.content)
            }
        }

    // ----------------------------------------------------------- tool events

    /**
     * Called by each tool after it runs (inside the conversation's tool loop, off the model's
     * generation). Builds a TOOL message and hands it to the tool-message flow, whose single
     * collector persists and appends it in order.
     */
    private fun recordToolInvocation(toolName: String, argsJson: String, resultJson: String) {
        val s = _state.value
        val conversationId = s.conversationId ?: return
        if (!s.isGenerating) return
        toolMessages.tryEmit(
            ChatMessage(
                id = newId(),
                conversationId = conversationId,
                role = MessageRole.TOOL,
                toolName = toolName,
                toolArgs = argsJson,
                toolResult = resultJson,
                createdAt = epochMillis(),
            ),
        )
    }
}
