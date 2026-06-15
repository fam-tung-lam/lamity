package com.phamtunglam.lamity.feature.skills.domain

import kotlinx.serialization.Serializable

/**
 * A skill: named instructions the model can pull in on demand via load_skill. A skill is only used
 * by a chat when the active agent attaches it — there is no global enabled flag.
 */
@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String = "",
    val instructions: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
