package com.phamtunglam.lamity.feature.studio.domain

import kotlinx.serialization.Serializable

/** An AI agent: persona + system prompt + attached tools and skills. */
@Serializable
data class Agent(
    val id: String,
    val name: String,
    val description: String = "",
    val systemPrompt: String = "",
    val toolIds: List<String> = emptyList(),
    val skillIds: List<String> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

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
