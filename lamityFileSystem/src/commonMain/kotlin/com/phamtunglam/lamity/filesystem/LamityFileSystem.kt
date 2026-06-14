package com.phamtunglam.lamity.filesystem

import com.phamtunglam.lamity.filesystem.models.LamityFileMetadata
import com.phamtunglam.lamity.filesystem.models.LamityFileSystemException
import com.phamtunglam.lamity.filesystem.models.LamityPath

/**
 * A small, Kotlin-friendly file system abstraction implemented per platform
 * (`java.io` on Android, Foundation/`NSFileManager` on iOS).
 *
 * Other modules depend on this interface rather than the platform APIs, so file
 * access is uniform and mockable. Obtain the platform-backed implementation with
 * [lamityFileSystem]; tests inject a fake or mock.
 *
 * ### Conventions
 * - Paths are [LamityPath]; build them with [LamityPath.div] (e.g. `dir / "file.txt"`).
 * - **Queries** ([exists], [metadataOrNull]) never throw for a missing entry —
 *   they answer `false` / `null`.
 * - **Reads and mutations** throw [LamityFileSystemException] on failure; the
 *   `…OrNull` extensions wrap the common best-effort cases.
 * - Writes create missing parent directories and, when `atomically` is set
 *   (the default), publish the file in one step so readers never observe a
 *   half-written file.
 *
 * Calls are **blocking**; move large transfers off the main thread (e.g. a
 * background dispatcher), exactly as you would with the underlying platform APIs.
 */
interface LamityFileSystem {

    /** Whether anything (file or directory) exists at [path]. */
    fun exists(path: LamityPath): Boolean

    /** Attributes of [path], or `null` if nothing exists there. */
    fun metadataOrNull(path: LamityPath): LamityFileMetadata?

    /**
     * Immediate children of the directory at [path] (not recursive), in no
     * guaranteed order. Throws [LamityFileSystemException] if [path] is not a
     * readable directory.
     */
    fun list(path: LamityPath): List<LamityPath>

    /**
     * Creates the directory at [path] and any missing parents. Succeeds silently
     * if it already exists as a directory; throws [LamityFileSystemException] if
     * it cannot be created (for example, a file already occupies the path).
     */
    fun createDirectories(path: LamityPath)

    /** Reads the entire file at [path] as bytes. Throws [LamityFileSystemException] if it cannot be read. */
    fun readBytes(path: LamityPath): ByteArray

    /** Reads the entire file at [path] as UTF-8 text. Throws [LamityFileSystemException] if it cannot be read. */
    fun readText(path: LamityPath): String

    /**
     * Writes [bytes] to [path], creating parent directories. When [atomically]
     * is `true` the file is published in a single step. Throws
     * [LamityFileSystemException] on failure.
     */
    fun writeBytes(path: LamityPath, bytes: ByteArray, atomically: Boolean = true)

    /**
     * Writes [text] to [path] as UTF-8, creating parent directories. When
     * [atomically] is `true` the file is published in a single step. Throws
     * [LamityFileSystemException] on failure.
     */
    fun writeText(path: LamityPath, text: String, atomically: Boolean = true)

    /**
     * Deletes the file or empty directory at [path]. A missing [path] is a no-op
     * unless [mustExist] is `true`. Throws [LamityFileSystemException] if a
     * present entry cannot be removed. Use [deleteRecursively] for non-empty
     * directories.
     */
    fun delete(path: LamityPath, mustExist: Boolean = false)

    /**
     * Deletes [path] and, if it is a directory, its entire subtree. A missing
     * [path] is a no-op unless [mustExist] is `true`. Throws
     * [LamityFileSystemException] if the tree cannot be fully removed.
     */
    fun deleteRecursively(path: LamityPath, mustExist: Boolean = false)

    /**
     * Moves [source] to [destination], creating destination parents. When an
     * entry already exists at [destination] it is replaced if [overwrite] is
     * `true`, otherwise the call throws. Throws [LamityFileSystemException] on
     * failure.
     */
    fun move(source: LamityPath, destination: LamityPath, overwrite: Boolean = true)

    /**
     * Copies [source] to [destination], creating destination parents. When an
     * entry already exists at [destination] it is replaced if [overwrite] is
     * `true`, otherwise the call throws. Throws [LamityFileSystemException] on
     * failure.
     */
    fun copy(source: LamityPath, destination: LamityPath, overwrite: Boolean = true)
}
