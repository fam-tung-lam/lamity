package com.phamtunglam.lamity.feature.skills.domain

import com.phamtunglam.lamity.feature.skills.data.SkillsRepository

/**
 * Deletes a skill. Any agent ⇄ skill links are removed automatically by the cascade on the
 * `agent_skills` junction's foreign key, so no manual detach is needed.
 */
class DeleteSkillUseCase(private val skills: SkillsRepository) {
    suspend operator fun invoke(skillId: String) {
        skills.delete(skillId)
    }
}
