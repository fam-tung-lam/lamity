package com.phamtunglam.lamity.llm.model

/** The role of a [Message] in a conversation. */
enum class Role(val jsonName: String) {
    System("system"),
    User("user"),
    Model("model"),
    Tool("tool"),
    ;

    companion object {
        fun fromJsonName(name: String?): Role = entries.firstOrNull { it.jsonName == name } ?: User
    }
}
