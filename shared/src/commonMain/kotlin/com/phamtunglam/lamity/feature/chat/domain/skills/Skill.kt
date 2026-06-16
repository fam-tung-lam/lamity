package com.phamtunglam.lamity.feature.chat.domain.skills

import kotlinx.serialization.Serializable

/**
 * A skill: named instructions the model can pull in on demand via the load_skill tool. Skills are
 * built-in ([BuiltinSkills]) and toggled per chat in the chat settings sheet; default all enabled.
 */
@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String = "",
    val instructions: String = "",
)
