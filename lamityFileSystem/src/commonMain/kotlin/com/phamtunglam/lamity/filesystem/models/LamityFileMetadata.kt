package com.phamtunglam.lamity.filesystem.models

/**
 * A snapshot of a file system entry's attributes, as returned by
 * [com.phamtunglam.lamity.filesystem.LamityFileSystem.metadataOrNull].
 */
data class LamityFileMetadata(
    /** Whether the entry is a regular file (not a directory, symlink target, etc.). */
    val isRegularFile: Boolean,

    /** Whether the entry is a directory. */
    val isDirectory: Boolean,

    /** Size in bytes. Meaningful for regular files; `0` for directories. */
    val sizeBytes: Long,

    /** Last-modified time in milliseconds since the Unix epoch, or `null` if the platform does not report it. */
    val lastModifiedEpochMillis: Long?,
)
