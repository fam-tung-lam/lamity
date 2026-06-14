package com.phamtunglam.lamity.filesystem.models

/** The kind of operation that produced a [LamityFileSystemException]. */
enum class LamityFileOperation {
    Read,
    Write,
    Delete,
    Move,
    Copy,
    CreateDirectories,
    List,
}

/**
 * Thrown by [com.phamtunglam.lamity.filesystem.LamityFileSystem] when a mutating
 * or reading operation cannot be completed.
 *
 * Carries the [operation] and [path] that failed so callers get an actionable
 * message without unpacking the platform [cause] (an `IOException` on Android, a
 * Foundation error on iOS).
 */
class LamityFileSystemException(
    val path: LamityPath,
    val operation: LamityFileOperation,
    detail: String? = null,
    cause: Throwable? = null,
) : RuntimeException(
    buildString {
        append("File system ").append(operation).append(" failed for '").append(path.value).append('\'')
        if (detail != null) append(": ").append(detail)
    },
    cause,
)
