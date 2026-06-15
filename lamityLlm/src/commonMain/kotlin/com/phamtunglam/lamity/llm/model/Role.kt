package com.phamtunglam.lamity.llm.model

/** The role of a [Message] in a conversation. */
enum class Role(val jsonName: String) {
    System("system"),
    User("user"),
    Model("model"),
    Tool("tool"),
    ;

    companion object {
        /** Maps a JSON role name to a [Role]; `"assistant"` is accepted as an alias for [Model]. */
        fun fromJsonName(name: String?): Role =
            entries.firstOrNull { it.jsonName == name }
                ?: if (name == "assistant") Model else User
    }
}
