package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.Contents
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.Role
import com.phamtunglam.lamity.llm.model.ToolCall
import com.phamtunglam.lamity.llm.native.ConversationHandle
import com.phamtunglam.lamity.llm.native.ConversationNativeRuntime
import com.phamtunglam.lamity.llm.native.TurnCallback
import com.phamtunglam.lamity.llm.tool.ToolManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

private const val RECURRING_TOOL_CALL_LIMIT = 25

/**
 * A LiteRT-LM conversation.
 */
class Conversation internal constructor(
    private val runtime: ConversationNativeRuntime,
    handle: ConversationHandle,
    private val toolManager: ToolManager,
    private val automaticToolCalling: Boolean = true,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var handle: ConversationHandle? = handle

    val isAlive: Boolean get() = handle != null

    private fun requireHandle(): ConversationHandle =
        handle ?: throw LiteRtLmException("Conversation has been disposed")

    /**
     * Sends [message] and streams the response as a flow of [Message] chunks. When
     * [automaticToolCalling] is enabled and the model requests tools, they run via the
     * [ToolManager] and the conversation continues, up to [RECURRING_TOOL_CALL_LIMIT] rounds.
     * Otherwise, tool calls are surfaced to the collector and the turn ends.
     */
    fun sendMessageStream(message: Message, extraContext: JsonObject? = null): Flow<Message> =
        channelFlow {
            val handle = requireHandle()
            val producer = this
            var current = message
            var iterations = 0
            var completedNormally = false
            try {
                while (true) {
                    if (iterations++ >= RECURRING_TOOL_CALL_LIMIT) {
                        throw LiteRtLmException(
                            "Exceeded recurring tool call limit ($RECURRING_TOOL_CALL_LIMIT)",
                        )
                    }
                    val pending = mutableListOf<ToolCall>()
                    val turn = CompletableDeferred<Unit>()
                    runtime.streamTurn(
                        handle,
                        current,
                        extraContext,
                        object : TurnCallback {
                            override fun onChunk(message: Message) {
                                if (automaticToolCalling && message.toolCalls.isNotEmpty()) {
                                    pending += message.toolCalls
                                }
                                val surfaceToolCalls =
                                    !automaticToolCalling && message.toolCalls.isNotEmpty()
                                if (!message.contents.isEmpty ||
                                    message.channels.isNotEmpty() ||
                                    surfaceToolCalls
                                ) {
                                    producer.trySend(message)
                                }
                            }

                            override fun onComplete() {
                                turn.complete(Unit)
                            }

                            override fun onError(message: String) {
                                turn.completeExceptionally(LiteRtLmException(message))
                            }
                        },
                    )
                    turn.await()
                    if (pending.isEmpty()) {
                        completedNormally = true
                        break
                    }
                    current = toolManager.handleToolCalls(pending)
                }
            } finally {
                if (!completedNormally) runtime.cancel(handle)
            }
        }.buffer(Channel.UNLIMITED)

    /** Sends [message] and returns the merged final response. */
    suspend fun sendMessage(message: Message, extraContext: JsonObject? = null): Message {
        val text = StringBuilder()
        val channels = mutableMapOf<String, String>()
        val toolCalls = mutableListOf<ToolCall>()
        sendMessageStream(message, extraContext).collect { chunk ->
            text.append(chunk.text)
            channels.putAll(chunk.channels)
            toolCalls += chunk.toolCalls
        }
        return Message(Role.Model, Contents.text(text.toString()), toolCalls = toolCalls, channels = channels)
    }

    /** Cancels any ongoing generation. */
    fun cancel() {
        val handleLocal = handle
        if (handleLocal != null) {
            runtime.cancel(handleLocal)
        }
    }

    /** Number of tokens currently in the conversation KV cache. */
    suspend fun getTokenCount(): Int = withContext(dispatcher) { runtime.getTokenCount(requireHandle()) }

    /** Benchmark information for this conversation (requires a benchmark-enabled engine). */
    suspend fun getBenchmarkInfo(): BenchmarkInfo =
        withContext(dispatcher) { runtime.getBenchmarkInfo(requireHandle()) }

    /** Renders [message] into the model's prompt string (for debugging/inspection). */
    suspend fun renderMessageIntoString(message: Message): String =
        withContext(dispatcher) { runtime.renderMessage(requireHandle(), message) }

    /** Releases the native conversation. */
    fun dispose() {
        val handle = this.handle ?: return
        this.handle = null
        runtime.deleteConversation(handle)
    }
}
