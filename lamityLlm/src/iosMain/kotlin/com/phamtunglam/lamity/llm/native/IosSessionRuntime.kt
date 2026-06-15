@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmResponses
import cnames.structs.LiteRtLmSession
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputData
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputDataType.kLiteRtLmInputDataTypeAudio
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputDataType.kLiteRtLmInputDataTypeImage
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputDataType.kLiteRtLmInputDataTypeText
import com.phamtunglam.lamity.llm.cinterop.litert_lm_responses_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_responses_get_num_candidates
import com.phamtunglam.lamity.llm.cinterop.litert_lm_responses_get_response_text_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_cancel_process
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_generate_content
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_generate_content_stream
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_run_decode
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_run_prefill
import com.phamtunglam.lamity.llm.model.InputData
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

internal actual fun createSessionNativeRuntime(): SessionNativeRuntime = IosSessionRuntime()

/**
 * [SessionNativeRuntime] over the `CLiteRTLM` C API via Kotlin/Native cinterop. Session content
 * streaming yields raw text chunks (unlike conversation streaming, which yields Message JSON).
 */
@Suppress("UNCHECKED_CAST")
internal class IosSessionRuntime : SessionNativeRuntime {
    override fun deleteSession(handle: SessionHandle) {
        litert_lm_session_delete(handle.native as CPointer<LiteRtLmSession>)
    }

    override fun runSessionPrefill(session: SessionHandle, inputData: List<InputData>) {
        val sessionPtr = session.native as CPointer<LiteRtLmSession>
        val status =
            memScoped {
                withInputData(
                    inputData,
                ) { inputs, count -> litert_lm_session_run_prefill(sessionPtr, inputs, count) }
            }
        if (status != 0) throw LiteRtLmException("Session prefill failed (status $status)")
    }

    override fun runSessionDecode(session: SessionHandle): String =
        responsesFirstText(litert_lm_session_run_decode(session.native as CPointer<LiteRtLmSession>))

    override fun generateSessionContent(session: SessionHandle, inputData: List<InputData>): String {
        val sessionPtr = session.native as CPointer<LiteRtLmSession>
        val responses =
            memScoped {
                withInputData(
                    inputData,
                ) { inputs, count -> litert_lm_session_generate_content(sessionPtr, inputs, count) }
            }
        return responsesFirstText(responses)
    }

    override fun generateSessionContentStream(
        session: SessionHandle,
        inputData: List<InputData>,
        callback: SessionStreamCallback,
    ) {
        val sessionPtr = session.native as CPointer<LiteRtLmSession>
        val ref = StableRef.create(IosSessionContext(callback))
        val status =
            memScoped {
                withInputData(inputData) { inputs, count ->
                    litert_lm_session_generate_content_stream(
                        sessionPtr,
                        inputs,
                        count,
                        staticCFunction(::iosSessionStreamCallback),
                        ref.asCPointer(),
                    )
                }
            }
        if (status != 0) {
            ref.dispose()
            callback.onError("Failed to start session stream (status $status)")
        }
    }

    override fun cancelSession(session: SessionHandle) {
        litert_lm_session_cancel_process(session.native as CPointer<LiteRtLmSession>)
    }

    private fun responsesFirstText(responses: CPointer<LiteRtLmResponses>?): String {
        if (responses == null) throw LiteRtLmException("LiteRT-LM returned no responses")
        try {
            if (litert_lm_responses_get_num_candidates(responses) <= 0) return ""
            return litert_lm_responses_get_response_text_at(responses, 0)?.toKString() ?: ""
        } finally {
            litert_lm_responses_delete(responses)
        }
    }

    private fun <T> MemScope.withInputData(
        inputData: List<InputData>,
        block: (CValuesRef<LiteRtLmInputData>?, ULong) -> T,
    ): T {
        if (inputData.isEmpty()) return block(null, 0uL)
        val array = allocArray<LiteRtLmInputData>(inputData.size)
        inputData.forEachIndexed { index, input -> fillInputData(array[index], input) }
        return block(array, inputData.size.convert())
    }

    private fun MemScope.fillInputData(slot: LiteRtLmInputData, input: InputData) {
        val (type, bytes) =
            when (input) {
                is InputData.Text -> kLiteRtLmInputDataTypeText to input.text.encodeToByteArray()
                is InputData.ImageBytes -> kLiteRtLmInputDataTypeImage to input.bytes
                is InputData.AudioBytes -> kLiteRtLmInputDataTypeAudio to input.bytes
            }
        slot.type = type
        slot.size = bytes.size.convert()
        val buffer = allocArray<ByteVar>(if (bytes.isEmpty()) 1 else bytes.size)
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned -> memcpy(buffer, pinned.addressOf(0), bytes.size.convert()) }
        }
        slot.data = buffer
    }
}
