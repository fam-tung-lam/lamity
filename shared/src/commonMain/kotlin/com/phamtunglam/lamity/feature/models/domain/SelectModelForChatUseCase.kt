package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.feature.settings.data.SettingsRepository

/**
 * Persists [model] as the chat's selected model. A freshly opened chat restores this selection from
 * settings, and an open chat observes the change reactively.
 */
class SelectModelForChatUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(model: LlmModel) = settings.setLastModelId(model.id)
}
