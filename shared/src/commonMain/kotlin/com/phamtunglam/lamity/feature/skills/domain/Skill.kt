package com.phamtunglam.lamity.feature.skills.domain

import kotlinx.serialization.Serializable

/** A skill: named instructions the model can pull in on demand via load_skill. */
@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String = "",
    val instructions: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
