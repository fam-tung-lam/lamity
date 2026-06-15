package com.phamtunglam.lamity.feature.chat.domain

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.feature.tools.domain.LoadSkillTool
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ChatSessionState(
    val conversationId: String? = null,
    val agentId: String? = null,
    val modelId: String? = null,
    /**
     * Effective inference config in use. For an agent it is derived from the agent (its override or
     * its model's catalog default); for an agent-less chat it is the model's catalog default, freely
     * adjustable in the customize sheet but never persisted.
     */
    val runtimeConfig: ModelConfig = ModelConfig(),
    /** Per-chat system prompt when running without an agent (in-memory only). */
    val customSystemPrompt: String? = null,
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
 *
 * Model/agent selection is live app state (restored from settings), not stored on the conversation.
 * Tools and skills are sourced exclusively from the selected agent — an agent-less chat runs a plain
 * model with an optional per-chat system prompt and no tools or skills. Inference config is the
 * agent's when one is selected, otherwise an in-memory default that the user can tweak per session.
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

    /** Serializes native session lifecycle so prepare and generate never race on [sessionHandle]. */
    private val sessionMutex = Mutex()
    private var prepareJob: Job? = null
    private var preparePending = false

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
            _state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
            // Warm the engine for the restored selection once everything has loaded.
            prepareSession()
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
        // Re-warm when agents change; refresh a selected agent's config in case it was just edited.
        scope.launch {
            agents.agents
                .drop(1)
                .collect {
                    if (agents.byId(_state.value.agentId) != null) {
                        _state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
                    }
                    prepareSession()
                }
        }
        // Re-warm when the catalog changes (e.g. a custom model was added/removed).
        scope.launch {
            models.models
                .drop(1)
                .collect { prepareSession() }
        }
    }

    // ----------------------------------------------------------- selection

    fun selectModel(modelId: String) {
        if (modelId == _state.value.modelId) return
        stopGeneration()
        _state.update { it.copy(modelId = modelId, error = null) }
        _state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
        persistSelection()
        prepareSession()
    }

    fun selectAgent(agentId: String?) {
        if (agentId == _state.value.agentId) return
        stopGeneration()
        _state.update { it.copy(agentId = agentId, error = null) }
        _state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
        persistSelection()
        prepareSession()
    }

    fun setCustomSystemPrompt(prompt: String) {
        _state.update { it.copy(customSystemPrompt = prompt.ifBlank { null }) }
        prepareSession()
    }

    /** Adjusts the in-memory config for an agent-less chat (not persisted). */
    fun setRuntimeConfig(config: ModelConfig) {
        if (config == _state.value.runtimeConfig) return
        _state.update { it.copy(runtimeConfig = config) }
        prepareSession()
    }

    private fun persistSelection() {
        val s = _state.value
        scope.launch { settings.setLastSelection(s.modelId, s.agentId) }
    }

    /** The effective config for the current selection: the agent's, or the chat model's catalog default. */
    private fun resolveDefaultConfig(): ModelConfig {
        val agent = agents.byId(_state.value.agentId)
        return if (agent != null) {
            agent.modelConfig ?: models.byId(agent.modelId)?.config ?: ModelConfig()
        } else {
            models.byId(_state.value.modelId)?.config ?: ModelConfig()
        }
    }

    // ----------------------------------------------------------- preparation

    /**
     * Eagerly loads the engine and (re)builds the native conversation for the current selection so
     * the first message streams without an upfront load. Idempotent — [ModelRuntime.ensureEngine]
     * and [ensureSession] dedupe by key/signature — and a no-op while generating. Coalesces rapid
     * calls and re-runs once if the selection changed mid-load.
     */
    @Suppress("TooGenericExceptionCaught")
    fun prepareSession() {
        if (prepareJob?.isActive == true) {
            preparePending = true
            return
        }
        prepareJob =
            scope.launch {
                do {
                    preparePending = false
                    try {
                        doPrepare()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.e(e) { "session prepare failed" }
                    }
                } while (preparePending)
            }
    }

    private suspend fun doPrepare() {
        if (_state.value.isGenerating) return
        val agent = agents.byId(_state.value.agentId)
        val model = resolveModel(agent) ?: return
        if (!downloads.isDownloaded(model)) return
        val config = loadEngineWithFallback(model, _state.value.runtimeConfig) ?: return
        ensureSession(model, config, agent, _state.value.conversationId)
    }

    /** The model to run: the agent's own model, or the chat-selected model when agent-less. */
    private fun resolveModel(agent: Agent?): LlmModel? =
        if (agent != null) models.byId(agent.modelId) else models.byId(_state.value.modelId)

    // ----------------------------------------------------------- lifecycle

    fun newChat() {
        stopGeneration()
        _state.update {
            it.copy(
                conversationId = null,
                messages = emptyList(),
                streamingText = "",
                streamingThought = "",
                error = null,
            )
        }
        // The native conversation is rebuilt by prepare/send: the empty history + null id give a
        // different session signature, so ensureSession discards the old one.
        prepareSession()
    }

    fun openConversation(conversationId: String) {
        val conversation = conversations.byId(conversationId) ?: return
        stopGeneration()
        scope.launch {
            val messages = conversations.loadMessages(conversationId)
            // The model/agent selection and config stay as-is; only the thread loads.
            _state.update {
                it.copy(
                    conversationId = conversation.id,
                    messages = messages,
                    streamingText = "",
                    streamingThought = "",
                    error = null,
                )
            }
            prepareSession()
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
        val agent = agents.byId(_state.value.agentId)
        val model = resolveModel(agent)
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
                    runGeneration(agent, model, trimmed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.e(e) { "generation failed" }
                    _state.update { it.copy(error = ChatError.Raw(e.message ?: "Generation failed")) }
                } finally {
                    _state.update { it.copy(isGenerating = false) }
                    // Re-warm for the next turn (cheap: engine + session dedupe).
                    prepareSession()
                }
            }
    }

    fun stopGeneration() {
        generateJob?.cancel()
        generateJob = null
    }

    private suspend fun runGeneration(agent: Agent?, model: LlmModel, text: String) {
        val config = loadEngineWithFallback(model, _state.value.runtimeConfig) ?: return

        // Conversation row (history) exists before the first token.
        val conversationId =
            _state.value.conversationId ?: run {
                val created = conversations.create()
                _state.update { it.copy(conversationId = created.id) }
                created.id
            }
        conversations.ensureTitle(conversationId, text)

        ensureSession(model, config, agent, conversationId).getOrElse {
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

        streamAndPersist(model, config, conversationId, text)
    }

    private suspend fun ensureEngineFor(model: LlmModel, config: ModelConfig): Result<Unit> {
        val engineKey = "${model.id}|${config.backend.name}|${config.maxTokens}"
        val engineConfig =
            EngineConfig(
                modelPath = downloads.modelPath(model),
                backend = if (config.backend == LlmBackend.GPU) Backend.Gpu() else Backend.Cpu(),
                maxNumTokens = config.maxTokens,
                cacheDir = dirs.cacheDir,
            )
        return runtime.ensureEngine(model.id, engineKey, engineConfig)
    }

    /**
     * Loads the engine for [model] with [config], transparently retrying once on the CPU backend when
     * a GPU load fails with a recoverable native error (see [shouldFallBackToCpu] — e.g. the Gemma 4
     * E-series `llm_litert_compiled_model_executor` failures). A successful CPU fallback updates the
     * in-memory [ChatSessionState.runtimeConfig] (never persisted) and shows a
     * [ChatNotice.SWITCHED_TO_CPU]. Returns the effective config to use, or null after setting
     * [ChatSessionState.error] on failure.
     */
    private suspend fun loadEngineWithFallback(model: LlmModel, config: ModelConfig): ModelConfig? {
        val first = ensureEngineFor(model, config)
        if (first.isSuccess) return config

        val cause = first.exceptionOrNull()
        if (!shouldFallBackToCpu(config.backend, cause?.message)) {
            _state.update { it.copy(error = loadError(cause?.message)) }
            return null
        }

        log.w { "GPU load failed for ${model.id}; retrying on CPU: ${cause?.message}" }
        val cpuConfig = config.copy(backend = LlmBackend.CPU)
        if (ensureEngineFor(model, cpuConfig).isFailure) {
            _state.update { it.copy(error = ChatError.Known(ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE)) }
            return null
        }
        _state.update { it.copy(runtimeConfig = cpuConfig, notice = ChatNotice.SWITCHED_TO_CPU, error = null) }
        return cpuConfig
    }

    private suspend fun streamAndPersist(
        model: LlmModel,
        config: ModelConfig,
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
                        handleGenerationError(model, config, hadOutput = charCount > 0, message = event.message)
                    }
                }
            }
        } catch (e: CancellationException) {
            // Stop pressed: keep whatever streamed so far. NonCancellable so the
            // persistence writes still run inside the cancelled coroutine.
            if (!completed) {
                withContext(NonCancellable) {
                    finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                    conversations.touch(conversationId)
                }
            }
            throw e
        }
        conversations.touch(conversationId)
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
     * recoverable native error, the config is transparently switched to CPU (in-memory only) and a
     * [ChatNotice.SWITCHED_TO_CPU] is shown so the next send runs on CPU; otherwise the error is
     * mapped to a user-facing [ChatError].
     */
    private fun handleGenerationError(
        model: LlmModel,
        config: ModelConfig,
        hadOutput: Boolean,
        message: String,
    ) {
        if (!hadOutput && shouldFallBackToCpu(config.backend, message)) {
            log.w { "GPU generation failed for ${model.id}; switching to CPU: $message" }
            _state.update {
                it.copy(
                    runtimeConfig = config.copy(backend = LlmBackend.CPU),
                    notice = ChatNotice.SWITCHED_TO_CPU,
                    error = null,
                )
            }
            return
        }
        _state.update { it.copy(error = loadError(message)) }
    }

    // ----------------------------------------------------------- session

    private suspend fun ensureSession(
        model: LlmModel,
        config: ModelConfig,
        agent: Agent?,
        conversationId: String?,
    ): Result<Unit> =
        sessionMutex.withLock {
            val skillsForAgent = effectiveSkills(agent)
            val toolIds = effectiveToolIds(agent, skillsForAgent)
            val systemPrompt = buildSystemPrompt(agent, skillsForAgent)
            val signature =
                sessionSignatureOf(model, config, agent, conversationId, toolIds, skillsForAgent, systemPrompt)
            if (sessionHandle != null && sessionSignature == signature) return@withLock Result.success(Unit)

            closeSession()

            val history = _state.value.messages.filter { it.role != MessageRole.TOOL }
            val conversationConfig =
                ConversationConfig(
                    systemMessage = systemPrompt?.let { Message.system(it) },
                    initialMessages = historyMessages(history),
                    tools = sessionTools(toolIds, skillsForAgent),
                    samplerConfig =
                        SamplerConfig(
                            topK = config.topK,
                            topP = config.topP,
                            temperature = config.temperature,
                        ),
                )
            runtime.createConversation(conversationConfig).map { handle ->
                sessionHandle = handle
                sessionSignature = signature
            }
        }

    private fun closeSession() {
        sessionHandle?.let { runtime.closeConversation(it) }
        sessionHandle = null
        sessionSignature = null
    }

    /** Skills attached to the active agent; an agent-less chat has none. */
    private fun effectiveSkills(agent: Agent?): List<Skill> {
        val ids = agent?.skillIds ?: return emptyList()
        return ids.mapNotNull { skills.byId(it) }
    }

    /** Tools attached to the active agent (plus load_skill when it has skills); none without an agent. */
    private fun effectiveToolIds(agent: Agent?, skillsForAgent: List<Skill>): List<String> {
        if (agent == null) return emptyList()
        val enabled =
            agent.toolIds.filter { id ->
                selectableTools.any { it.id == id } && id != LoadSkillTool.ID
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

    @Suppress("LongParameterList") // All inputs that distinguish one native session from another.
    private fun sessionSignatureOf(
        model: LlmModel,
        config: ModelConfig,
        agent: Agent?,
        conversationId: String?,
        toolIds: List<String>,
        skillsForAgent: List<Skill>,
        systemPrompt: String?,
    ): String =
        buildString {
            append(conversationId)
            append('|').append(model.id)
            append('|').append(config.backend).append(config.maxTokens)
            append('|').append(config.topK).append(config.topP).append(config.temperature)
            append('|').append(agent?.id).append(':').append(agent?.updatedAt)
            append('|').append(toolIds.sorted().joinToString(","))
            append('|').append(skillsForAgent.joinToString(",") { "${it.id}:${it.updatedAt}" })
            append('|').append(systemPrompt?.hashCode() ?: 0)
        }

    private fun buildSystemPrompt(agent: Agent?, skillsForAgent: List<Skill>): String? {
        val parts = mutableListOf<String>()
        val basePrompt = if (agent != null) agent.systemPrompt else _state.value.customSystemPrompt
        basePrompt?.takeIf { it.isNotBlank() }?.let { parts += it.trim() }
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
