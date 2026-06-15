package com.phamtunglam.lamity.llm.model

/** A conversation message. */
class Message(
    val role: Role,
    val contents: Contents = Contents.empty,
    val toolCalls: List<ToolCall> = emptyList(),
    val channels: Map<String, String> = emptyMap(),
) {
    /** Concatenated text of the message contents. */
    val text: String get() = contents.text

    companion object {
        fun system(text: String): Message = Message(Role.System, Contents.text(text))

        fun systemContents(contents: Contents): Message = Message(Role.System, contents)

        fun user(text: String): Message = Message(Role.User, Contents.text(text))

        fun userContents(contents: Contents): Message = Message(Role.User, contents)

        fun model(text: String): Message = Message(Role.Model, Contents.text(text))

        fun tool(contents: Contents): Message = Message(Role.Tool, contents)
    }
}
