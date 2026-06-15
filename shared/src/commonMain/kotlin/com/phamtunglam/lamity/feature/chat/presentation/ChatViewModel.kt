package com.phamtunglam.lamity.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.ChatError
import com.phamtunglam.lamity.feature.chat.domain.ChatMessage
import com.phamtunglam.lamity.feature.chat.domain.ChatNotice
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionFactory
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionState
import com.phamtunglam.lamity.feature.chat.domain.EngineLoad
import com.phamtunglam.lamity.feature.chat.domain.GenEvent
import com.phamtunglam.lamity.feature.chat.domain.LoadEngineUseCase
import com.phamtunglam.lamity.feature.chat.domain.MessageRole
import com.phamtunglam.lamity.feature.chat.domain.ModelRuntime
import com.phamtunglam.lamity.feature.chat.domain.loadError
import com.phamtunglam.lamity.feature.chat.domain.shouldFallBackToCpu
import com.phamtunglam.lamity.feature.models.data.ModelFiles
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.models.domain.ObserveModelStatusesUseCase
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ChatUiState(
    val chat: ChatSessionState = ChatSessionState(),
    val agents: List<Agent> = emptyList(),
    val models: List<LlmModel> = emptyList(),
    val selectedModelReady: Boolean = false,
)

/**
 * Orchestrates the chat screen for its lifetime: sessions, streaming, tool events and history
 * persistence, all on [viewModelScope]. Created when the chat opens (optionally for [conversationId])
 * and torn down when it closes — the warm native engine outlives it in [ModelRuntime].
 *
 * Model/agent selection is restored from settings; tools and skills come from the selected agent.
 */
@Suppress("TooManyFunctions") // Cohesive chat orchestrator; splitting would scatter related logic.
class ChatViewModel(
    private val conversationId: String?,
    private val runtime: ModelRuntime,
    private val conversations: ConversationsRepository,
    private val agents: AgentsRepository,
    private val models: ModelsRepository,
    private val settings: SettingsRepository,
    private val modelFiles: ModelFiles,
    private val loadEngine: LoadEngineUseCase,
    private val sessionFactory: ChatSessionFactory,
    observeStatuses: ObserveModelStatusesUseCase,
) : ViewModel() {
    private val log = Logger.withTag("ChatViewModel")

    private val state = MutableStateFlow(ChatSessionState())

    /** Tool invocations recorded during generation; persisted and shown as TOOL messages. */
    private val toolMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)

    private var sessionHandle: String? = null
    private var sessionSignature: String? = null
    private var generateJob: Job? = null

    /** Serializes native session lifecycle so prepare and generate never race on [sessionHandle]. */
    private val sessionMutex = Mutex()
    private var prepareJob: Job? = null
    private var preparePending = false

    val uiState: StateFlow<ChatUiState> =
        combine(
            state,
            agents.agents,
            models.models,
            observeStatuses(flowOf(Unit)),
        ) { chatState, agentList, modelList, _ ->
            // The effective model is the agent's when one with a model is selected, else the chat pick.
            val agent = agentList.firstOrNull { it.id == chatState.agentId }
            val effectiveModelId = agent?.modelId ?: chatState.modelId
            val selected = modelList.firstOrNull { it.id == effectiveModelId }
            ChatUiState(
                chat = chatState,
                agents = agentList,
                models = modelList,
                selectedModelReady = selected != null && modelFiles.isDownloaded(selected),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState(chat = state.value))

    init {
        viewModelScope.launch {
            settings.awaitLoaded()
            models.awaitLoaded()
            agents.awaitLoaded()
            val s = settings.value
            state.update {
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
            state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
            conversationId?.let { loadConversation(it) }
            // Warm the engine for the restored selection once everything has loaded.
            prepareSession()
        }
        viewModelScope.launch {
            runtime.engineState.collect { es -> state.update { it.copy(engine = es) } }
        }
        viewModelScope.launch {
            toolMessages.collect { message ->
                conversations.appendMessage(message)
                state.update { it.copy(messages = it.messages + message) }
            }
        }
        // Re-warm when agents change; refresh a selected agent's config in case it was just edited.
        viewModelScope.launch {
            agents.agents
                .drop(1)
                .collect { _ ->
                    if (agents.byId(state.value.agentId) != null) {
                        state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
                    }
                    prepareSession()
                }
        }
        // Re-warm when the catalog changes (e.g. a custom model was added/removed).
        viewModelScope.launch {
            models.models
                .drop(1)
                .collect { prepareSession() }
        }
        // Reset to a fresh chat if the open conversation is deleted elsewhere.
        viewModelScope.launch {
            conversations.conversations
                .drop(1)
                .collect { list ->
                    val id = state.value.conversationId ?: return@collect
                    if (list.none { it.id == id }) newChat()
                }
        }
    }

    override fun onCleared() {
        stopGeneration()
        closeSession()
    }

    // ----------------------------------------------------------- selection

    /** Warms the engine + native conversation for the current selection. Called when the chat opens. */
    fun prepare() = prepareSession()

    fun selectModel(modelId: String) {
        if (modelId == state.value.modelId) return
        stopGeneration()
        state.update { it.copy(modelId = modelId, error = null) }
        state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
        persistSelection()
        prepareSession()
    }

    fun selectAgent(agentId: String?) {
        if (agentId == state.value.agentId) return
        stopGeneration()
        state.update { it.copy(agentId = agentId, error = null) }
        state.update { it.copy(runtimeConfig = resolveDefaultConfig()) }
        persistSelection()
        prepareSession()
    }

    fun setCustomSystemPrompt(prompt: String) {
        state.update { it.copy(customSystemPrompt = prompt.ifBlank { null }) }
        prepareSession()
    }

    /** Adjusts the in-memory inference config for an agent-less chat (not persisted). */
    fun setRuntimeConfig(config: ModelConfig) {
        if (config == state.value.runtimeConfig) return
        state.update { it.copy(runtimeConfig = config) }
        prepareSession()
    }

    private fun persistSelection() {
        val s = state.value
        viewModelScope.launch { settings.setLastSelection(s.modelId, s.agentId) }
    }

    /** The effective config for the current selection: the agent's, or the chat model's catalog default. */
    private fun resolveDefaultConfig(): ModelConfig {
        val agent = agents.byId(state.value.agentId)
        return if (agent != null) {
            agent.modelConfig ?: models.byId(agent.modelId)?.config ?: ModelConfig()
        } else {
            models.byId(state.value.modelId)?.config ?: ModelConfig()
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
            viewModelScope.launch {
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
        if (state.value.isGenerating) return
        val agent = agents.byId(state.value.agentId)
        val model = resolveModel(agent) ?: return
        if (!modelFiles.isDownloaded(model)) return
        val config = loadEngineOrSetError(model, state.value.runtimeConfig) ?: return
        ensureSession(model, config, agent, state.value.conversationId)
    }

    /** The model to run: the agent's own model, or the chat-selected model when agent-less. */
    private fun resolveModel(agent: Agent?): LlmModel? =
        if (agent != null) models.byId(agent.modelId) else models.byId(state.value.modelId)

    // ----------------------------------------------------------- lifecycle

    fun newChat() {
        stopGeneration()
        state.update {
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

    private suspend fun loadConversation(conversationId: String) {
        val conversation = conversations.byId(conversationId) ?: return
        val messages = conversations.loadMessages(conversationId)
        // The model/agent selection and config stay as restored from settings; only the thread loads.
        state.update {
            it.copy(
                conversationId = conversation.id,
                messages = messages,
                streamingText = "",
                streamingThought = "",
                error = null,
            )
        }
    }

    fun dismissError() = state.update { it.copy(error = null) }

    fun dismissNotice() = state.update { it.copy(notice = null) }

    // ----------------------------------------------------------- generation

    // Surfaces any generation failure (including arbitrary native LLM errors) to the UI; cancellation is rethrown.
    @Suppress("TooGenericExceptionCaught")
    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || state.value.isGenerating) return
        val agent = agents.byId(state.value.agentId)
        val model = resolveModel(agent)
        if (model == null) {
            state.update { it.copy(error = ChatError.Raw("Select a model first")) }
            return
        }
        if (!modelFiles.isDownloaded(model)) {
            state.update { it.copy(error = ChatError.Raw("Model is not downloaded yet")) }
            return
        }

        generateJob =
            viewModelScope.launch {
                state.update {
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
                    state.update { it.copy(error = ChatError.Raw(e.message ?: "Generation failed")) }
                } finally {
                    state.update { it.copy(isGenerating = false) }
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
        val config = loadEngineOrSetError(model, state.value.runtimeConfig) ?: return

        // Conversation row (history) exists before the first token.
        val conversationId =
            state.value.conversationId ?: run {
                val created = conversations.create()
                state.update { it.copy(conversationId = created.id) }
                created.id
            }
        conversations.ensureTitle(conversationId, text)

        ensureSession(model, config, agent, conversationId).getOrElse {
            state.update { s -> s.copy(error = ChatError.Raw(it.message ?: "Could not start conversation")) }
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
        state.update { it.copy(messages = it.messages + userMessage) }

        streamAndPersist(model, config, conversationId, text)
    }

    /**
     * Loads the engine for [model]/[config], applying state effects: a recoverable GPU failure switches
     * the in-memory [ChatSessionState.runtimeConfig] to CPU (never persisted) and shows a
     * [ChatNotice.SWITCHED_TO_CPU]; an unrecoverable failure sets [ChatSessionState.error]. Returns the
     * effective config to use, or null on failure.
     */
    private suspend fun loadEngineOrSetError(model: LlmModel, config: ModelConfig): ModelConfig? =
        when (val result = loadEngine(model, config)) {
            is EngineLoad.Ready -> {
                result.config
            }

            is EngineLoad.SwitchedToCpu -> {
                state.update {
                    it.copy(runtimeConfig = result.config, notice = ChatNotice.SWITCHED_TO_CPU, error = null)
                }
                result.config
            }

            is EngineLoad.Failed -> {
                state.update { it.copy(error = result.error) }
                null
            }
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

        val handle = sessionHandle ?: return
        try {
            runtime.generate(handle, text).collect { event ->
                when (event) {
                    is GenEvent.Chunk -> {
                        if (firstTokenAt == 0L) firstTokenAt = epochMillis()
                        charCount += event.text.length
                        state.update { it.copy(streamingText = it.streamingText + event.text) }
                    }

                    is GenEvent.Thought -> {
                        if (firstTokenAt == 0L) firstTokenAt = epochMillis()
                        charCount += event.text.length
                        state.update { it.copy(streamingThought = it.streamingThought + event.text) }
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
        val s = state.value
        if (s.streamingText.isBlank() && s.streamingThought.isBlank()) {
            state.update { it.copy(streamingText = "", streamingThought = "") }
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
        state.update {
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
            state.update {
                it.copy(
                    runtimeConfig = config.copy(backend = LlmBackend.CPU),
                    notice = ChatNotice.SWITCHED_TO_CPU,
                    error = null,
                )
            }
            return
        }
        state.update { it.copy(error = loadError(message)) }
    }

    // ----------------------------------------------------------- session

    private suspend fun ensureSession(
        model: LlmModel,
        config: ModelConfig,
        agent: Agent?,
        conversationId: String?,
    ): Result<Unit> =
        sessionMutex.withLock {
            val built =
                sessionFactory.build(
                    model = model,
                    config = config,
                    agent = agent,
                    conversationId = conversationId,
                    customSystemPrompt = state.value.customSystemPrompt,
                    history = state.value.messages,
                    onToolInvoked = ::recordToolInvocation,
                )
            if (sessionHandle != null && sessionSignature == built.signature) return@withLock Result.success(Unit)

            closeSession()

            runtime.createConversation(built.config).map { handle ->
                sessionHandle = handle
                sessionSignature = built.signature
            }
        }

    private fun closeSession() {
        sessionHandle?.let { runtime.closeConversation(it) }
        sessionHandle = null
        sessionSignature = null
    }

    // ----------------------------------------------------------- tool events

    /**
     * Called by each tool after it runs (inside the conversation's tool loop, off the model's
     * generation). Builds a TOOL message and hands it to the tool-message flow, whose single
     * collector persists and appends it in order.
     */
    private fun recordToolInvocation(toolName: String, argsJson: String, resultJson: String) {
        val s = state.value
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
