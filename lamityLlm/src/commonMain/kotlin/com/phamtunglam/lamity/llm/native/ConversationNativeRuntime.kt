package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.Message
import kotlinx.serialization.json.JsonObject

/** Opaque platform conversation handle (Android `Conversation` / iOS C conversation pointer). */
internal class ConversationHandle(val native: Any)

internal expect fun createConversationNativeRuntime(): ConversationNativeRuntime

/**
 * Streaming, inspection, and teardown of a conversation created via [EngineNativeRuntime.createConversation].
 */
internal interface ConversationNativeRuntime {
    fun deleteConversation(handle: ConversationHandle)

    fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    )

    fun cancel(conversation: ConversationHandle)

    fun getTokenCount(conversation: ConversationHandle): Int

    fun getBenchmarkInfo(conversation: ConversationHandle): BenchmarkInfo

    fun renderMessage(conversation: ConversationHandle, message: Message): String
}
