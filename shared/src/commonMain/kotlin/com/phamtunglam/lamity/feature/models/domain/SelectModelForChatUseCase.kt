package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.feature.settings.data.SettingsRepository

/**
 * Persists [model] as the chat's selected model (keeping the current agent selection). A freshly
 * opened chat restores this selection from settings.
 */
class SelectModelForChatUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(model: LlmModel) = settings.setLastSelection(model.id, settings.value.lastAgentId)
}
