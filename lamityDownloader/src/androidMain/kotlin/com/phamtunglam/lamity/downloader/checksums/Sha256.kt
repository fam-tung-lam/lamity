package com.phamtunglam.lamity.downloader.checksums

import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.blackholeSink
import okio.buffer

internal object Sha256 {

    /** Lowercase hex SHA-256 digest of [path], streamed via okio. */
    fun of(path: Path, fileSystem: FileSystem = FileSystem.SYSTEM): String =
        fileSystem.source(path).use { source ->
            HashingSource.sha256(source).use { hashing ->
                hashing.buffer().readAll(blackholeSink())
                hashing.hash.hex()
            }
        }
}
