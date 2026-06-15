package com.phamtunglam.lamity.llm.native

/** Opaque platform engine handle (Android `Engine` / iOS C engine pointer). */
internal class EngineHandle(val native: Any)

/** Opaque platform conversation handle (Android `Conversation` / iOS C conversation pointer). */
internal class ConversationHandle(val native: Any)
