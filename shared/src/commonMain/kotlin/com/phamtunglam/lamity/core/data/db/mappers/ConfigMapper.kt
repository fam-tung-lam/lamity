package com.phamtunglam.lamity.core.data.db.mappers

import com.phamtunglam.lamity.core.data.db.entities.AgentConfigEntity
import com.phamtunglam.lamity.feature.models.domain.ModelConfig

/** Maps the domain [ModelConfig] to/from the flattened [AgentConfigEntity] columns. */
internal fun ModelConfig.toAgentConfigEntity(agentId: String): AgentConfigEntity =
    AgentConfigEntity(
        agentId = agentId,
        backend = backend,
        maxTokens = maxTokens,
        topK = topK,
        topP = topP,
        temperature = temperature,
    )

internal fun AgentConfigEntity.toModelConfig(): ModelConfig =
    ModelConfig(
        backend = backend,
        maxTokens = maxTokens,
        topK = topK,
        topP = topP,
        temperature = temperature,
    )
