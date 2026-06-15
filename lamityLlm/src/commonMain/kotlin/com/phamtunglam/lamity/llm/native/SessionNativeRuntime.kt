package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.InputData

/** Opaque platform session handle (Android `Session` / iOS C session pointer). */
internal class SessionHandle(val native: Any)

internal expect fun createSessionNativeRuntime(): SessionNativeRuntime

/**
 * Prefill/decode, content generation, and teardown of a session created via [EngineNativeRuntime.createSession].
 */
internal interface SessionNativeRuntime {
    fun deleteSession(handle: SessionHandle)

    fun runSessionPrefill(session: SessionHandle, inputData: List<InputData>)

    fun runSessionDecode(session: SessionHandle): String

    fun generateSessionContent(session: SessionHandle, inputData: List<InputData>): String

    fun generateSessionContentStream(
        session: SessionHandle,
        inputData: List<InputData>,
        callback: SessionStreamCallback,
    )

    fun cancelSession(session: SessionHandle)
}
