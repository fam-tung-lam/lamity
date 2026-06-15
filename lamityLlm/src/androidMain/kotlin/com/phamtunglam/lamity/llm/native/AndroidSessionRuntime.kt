@file:OptIn(ExperimentalApi::class)

package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.Session as SdkSession
import com.phamtunglam.lamity.llm.model.InputData

internal actual fun createSessionNativeRuntime(): SessionNativeRuntime = AndroidSessionRuntime()

/** [SessionNativeRuntime] over the `com.google.ai.edge.litertlm` Kotlin SDK. */
internal class AndroidSessionRuntime : SessionNativeRuntime {
    override fun deleteSession(handle: SessionHandle) {
        (handle.native as SdkSession).close()
    }

    override fun runSessionPrefill(session: SessionHandle, inputData: List<InputData>) {
        (session.native as SdkSession).runPrefill(inputData.map { it.toSdk() })
    }

    override fun runSessionDecode(session: SessionHandle): String = (session.native as SdkSession).runDecode()

    override fun generateSessionContent(session: SessionHandle, inputData: List<InputData>): String =
        (session.native as SdkSession).generateContent(inputData.map { it.toSdk() })

    override fun generateSessionContentStream(
        session: SessionHandle,
        inputData: List<InputData>,
        callback: SessionStreamCallback,
    ) {
        (session.native as SdkSession).generateContentStream(
            inputData.map { it.toSdk() },
            object : ResponseCallback {
                override fun onNext(response: String) {
                    callback.onChunk(response)
                }

                override fun onDone() {
                    callback.onComplete()
                }

                override fun onError(throwable: Throwable) {
                    callback.onError(throwable.message ?: throwable.toString())
                }
            },
        )
    }

    override fun cancelSession(session: SessionHandle) {
        (session.native as SdkSession).cancelProcess()
    }
}
