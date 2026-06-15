@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmConversation
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_cancel_process
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_get_benchmark_info
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_get_token_count
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_render_message_to_string
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_send_message_stream
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.serialization.toJsonString
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.JsonObject

internal actual fun createConversationNativeRuntime(): ConversationNativeRuntime = IosConversationRuntime()

/**
 * [ConversationNativeRuntime] over the `CLiteRTLM` C API via Kotlin/Native cinterop. JSON is the
 * wire format and tool calls are surfaced to the common [com.phamtunglam.lamity.llm.Conversation].
 */
@Suppress("UNCHECKED_CAST")
internal class IosConversationRuntime : ConversationNativeRuntime {
    override fun deleteConversation(handle: ConversationHandle) {
        litert_lm_conversation_delete(handle.native as CPointer<LiteRtLmConversation>)
    }

    override fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    ) {
        val conversationPtr = conversation.native as CPointer<LiteRtLmConversation>
        val ref = StableRef.create(IosTurnContext(callback))
        val optionalArgs = litert_lm_conversation_optional_args_create()
        val status =
            litert_lm_conversation_send_message_stream(
                conversationPtr,
                message.toJsonString(),
                extraContext?.toString(),
                optionalArgs,
                staticCFunction(::iosStreamCallback),
                ref.asCPointer(),
            )
        litert_lm_conversation_optional_args_delete(optionalArgs)
        if (status != 0) {
            ref.dispose()
            callback.onError("Failed to start stream (status $status)")
        }
    }

    override fun cancel(conversation: ConversationHandle) {
        litert_lm_conversation_cancel_process(conversation.native as CPointer<LiteRtLmConversation>)
    }

    override fun getTokenCount(conversation: ConversationHandle): Int =
        litert_lm_conversation_get_token_count(conversation.native as CPointer<LiteRtLmConversation>)

    override fun getBenchmarkInfo(conversation: ConversationHandle): BenchmarkInfo {
        val info =
            litert_lm_conversation_get_benchmark_info(conversation.native as CPointer<LiteRtLmConversation>)
                ?: throw LiteRtLmException("Could not get benchmark info")
        return readBenchmarkInfo(info)
    }

    override fun renderMessage(conversation: ConversationHandle, message: Message): String =
        litert_lm_conversation_render_message_to_string(
            conversation.native as CPointer<LiteRtLmConversation>,
            message.toJsonString(),
        )?.toKString() ?: throw LiteRtLmException("Failed to render message")
}
