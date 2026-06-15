package com.phamtunglam.lamity.llm.model

import kotlinx.serialization.json.JsonElement

/** A single piece of message content. */
sealed interface Content {
    data class Text(val text: String) : Content

    /** Image as raw bytes (commonly PNG/JPEG). */
    class ImageBytes(val bytes: ByteArray) : Content

    /** Image referenced by absolute file path. */
    data class ImageFile(val path: String) : Content

    /** Audio as raw bytes (commonly WAV). */
    class AudioBytes(val bytes: ByteArray) : Content

    /** Audio referenced by absolute file path. */
    data class AudioFile(val path: String) : Content

    /** A tool result returned to the model. */
    data class ToolResponse(val name: String, val response: JsonElement?) : Content
}
