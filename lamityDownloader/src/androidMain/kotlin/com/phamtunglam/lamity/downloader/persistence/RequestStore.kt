package com.phamtunglam.lamity.downloader.persistence

import android.content.Context
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.util.Base64

/**
 * Persists full [DownloadRequest]s as JSON files so workers and resumes can
 * recover them across process restarts (WorkManager `Data` has a 10 KB cap).
 */
internal class RequestStore(context: Context, private val fileSystem: FileSystem = FileSystem.SYSTEM) {
    private val dir: Path = context.filesDir.absolutePath.toPath() / STORE_DIR
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun save(request: DownloadRequest) {
        fileSystem.createDirectories(dir)
        fileSystem.write(fileFor(request.id)) {
            writeUtf8(json.encodeToString(request))
        }
    }

    fun load(id: String): DownloadRequest? {
        val file = fileFor(id)
        if (!fileSystem.exists(file)) return null
        return runCatching {
            json.decodeFromString<DownloadRequest>(fileSystem.read(file) { readUtf8() })
        }.getOrNull()
    }

    fun delete(id: String) {
        fileSystem.delete(fileFor(id), mustExist = false)
    }

    private fun fileFor(id: String): Path = dir / "${id.toUrlSafeBase64()}.json"

    private fun String.toUrlSafeBase64(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))

    private companion object {
        const val STORE_DIR = "lamity_downloads"
    }
}
