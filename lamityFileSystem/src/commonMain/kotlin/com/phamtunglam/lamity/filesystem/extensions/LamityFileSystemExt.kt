package com.phamtunglam.lamity.filesystem.extensions

import com.phamtunglam.lamity.filesystem.LamityFileSystem
import com.phamtunglam.lamity.filesystem.models.LamityPath

/** `true` if [path] exists and is a regular file. */
fun LamityFileSystem.isRegularFile(path: LamityPath): Boolean =
    metadataOrNull(path)?.isRegularFile == true

/** `true` if [path] exists and is a directory. */
fun LamityFileSystem.isDirectory(path: LamityPath): Boolean =
    metadataOrNull(path)?.isDirectory == true

/** Size of [path] in bytes, or `null` if nothing exists there. */
fun LamityFileSystem.sizeOrNull(path: LamityPath): Long? =
    metadataOrNull(path)?.sizeBytes

/** Reads [path] as UTF-8 text, or `null` if it is missing or unreadable. */
fun LamityFileSystem.readTextOrNull(path: LamityPath): String? =
    if (exists(path)) runCatching { readText(path) }.getOrNull() else null

/** Reads [path] as bytes, or `null` if it is missing or unreadable. */
fun LamityFileSystem.readBytesOrNull(path: LamityPath): ByteArray? =
    if (exists(path)) runCatching { readBytes(path) }.getOrNull() else null

/** Children of [path], or an empty list if it is not a readable directory. */
fun LamityFileSystem.listOrEmpty(path: LamityPath): List<LamityPath> =
    if (isDirectory(path)) runCatching { list(path) }.getOrDefault(emptyList()) else emptyList()
