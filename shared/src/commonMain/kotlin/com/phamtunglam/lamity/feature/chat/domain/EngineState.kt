package com.phamtunglam.lamity.feature.chat.domain

/** Reactive state of the loaded engine. */
sealed interface EngineState {
    data object Idle : EngineState

    data class Loading(val modelId: String) : EngineState

    data class Ready(val modelId: String, val engineKey: String) : EngineState

    data class Error(val modelId: String, val message: String) : EngineState
}
