package com.phamtunglam.lamity.downloader.checksums

import java.io.File
import java.security.MessageDigest

internal object Sha256 {

    /** Lowercase hex SHA-256 digest of [file], streamed in 64 KiB chunks. */
    fun of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}
