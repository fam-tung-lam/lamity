package com.phamtunglam.lamity.llm.native

/** Opaque platform engine handle (Android `Engine` / iOS C engine pointer). */
internal class EngineHandle(val native: Any)

/** Opaque platform conversation handle (Android `Conversation` / iOS C conversation pointer). */
internal class ConversationHandle(val native: Any)

/** Opaque platform session handle (Android `Session` / iOS C session pointer). */
internal class SessionHandle(val native: Any)

/** Opaque platform capabilities handle (Android `Capabilities` / iOS C loaded-file pointer). */
internal class CapabilitiesHandle(val native: Any)
