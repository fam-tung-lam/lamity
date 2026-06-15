package com.phamtunglam.lamity.llm.model

/** Input data for the lower-level [com.phamtunglam.lamity.llm.Session] API. */
sealed interface InputData {
    /** Text input. */
    data class Text(val text: String) : InputData

    /** Image input bytes. Supported formats depend on the model, commonly PNG and JPEG. */
    class ImageBytes(val bytes: ByteArray) : InputData

    /** Audio input bytes. Supported formats depend on the model, commonly WAV. */
    class AudioBytes(val bytes: ByteArray) : InputData

    companion object {
        fun text(text: String): InputData = Text(text)

        fun imageBytes(bytes: ByteArray): InputData = ImageBytes(bytes)

        fun audioBytes(bytes: ByteArray): InputData = AudioBytes(bytes)
    }
}
