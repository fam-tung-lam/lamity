@file:OptIn(ExperimentalApi::class)

package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.Conversation as SdkConversation
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message as SdkMessage
import com.google.ai.edge.litertlm.MessageCallback
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.Message
import kotlinx.serialization.json.JsonObject

internal actual fun createConversationNativeRuntime(): ConversationNativeRuntime = AndroidConversationRuntime()

/**
 * [ConversationNativeRuntime] over the `com.google.ai.edge.litertlm` Kotlin SDK. Tool calls are
 * surfaced to the common [com.phamtunglam.lamity.llm.Conversation] rather than executed natively.
 */
internal class AndroidConversationRuntime : ConversationNativeRuntime {
    override fun deleteConversation(handle: ConversationHandle) {
        (handle.native as SdkConversation).close()
    }

    override fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    ) {
        val conv = conversation.native as SdkConversation
        val extra = extraContext?.let { jsonObjectToMap(it) } ?: emptyMap()
        conv.sendMessageAsync(
            message.toSdk(),
            object : MessageCallback {
                override fun onMessage(message: SdkMessage) {
                    callback.onChunk(message.toCommon())
                }

                override fun onDone() {
                    callback.onComplete()
                }

                override fun onError(throwable: Throwable) {
                    callback.onError(throwable.message ?: throwable.toString())
                }
            },
            extra,
        )
    }

    override fun cancel(conversation: ConversationHandle) {
        (conversation.native as SdkConversation).cancelProcess()
    }

    override fun getTokenCount(conversation: ConversationHandle): Int =
        (conversation.native as SdkConversation).getTokenCount()

    override fun getBenchmarkInfo(conversation: ConversationHandle): BenchmarkInfo =
        (conversation.native as SdkConversation).getBenchmarkInfo().toCommon()

    override fun renderMessage(conversation: ConversationHandle, message: Message): String =
        (conversation.native as SdkConversation).renderMessageIntoString(message.toSdk(), emptyMap())
}
