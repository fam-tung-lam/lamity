package com.phamtunglam.lamity.llm.model

/** Configuration for LoRA weights applied to a session or conversation. */
data class LoraConfig(val loraPath: String? = null, val audioLoraPath: String? = null)
