package com.phamtunglam.lamity.filesystem

/**
 * The platform-backed [LamityFileSystem] (`java.io` on Android, `NSFileManager`
 * on iOS).
 *
 * Implementations are stateless and cheap to create; hold a single instance
 * (typically a DI singleton) and share it.
 */
expect fun lamityFileSystem(): LamityFileSystem
