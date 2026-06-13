package com.phamtunglam.lamity.feature.studio.data

import com.phamtunglam.lamity.feature.studio.domain.Skill
import kotlinx.coroutines.flow.StateFlow

interface SkillsRepository {
    val skills: StateFlow<List<Skill>>

    /** Completes once persisted skills have been loaded into [skills]. */
    suspend fun awaitLoaded()

    fun byId(id: String?): Skill?

    suspend fun upsert(
        id: String?,
        name: String,
        description: String,
        instructions: String,
        enabled: Boolean,
    ): Skill

    suspend fun setEnabled(skillId: String, enabled: Boolean)

    suspend fun delete(skillId: String)
}
