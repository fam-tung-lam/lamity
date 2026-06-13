package com.phamtunglam.lamity.downloader.persistence

import android.content.Context
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import java.io.File
import java.util.Base64
import kotlinx.serialization.json.Json

/**
 * Persists full [DownloadRequest]s as JSON files so workers and resumes can
 * recover them across process restarts (WorkManager `Data` has a 10 KB cap).
 */
internal class RequestStore(context: Context) {

    private val dir = File(context.filesDir, STORE_DIR)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun save(request: DownloadRequest) {
        dir.mkdirs()
        fileFor(request.id).writeText(json.encodeToString(request))
    }

    fun load(id: String): DownloadRequest? =
        fileFor(id).takeIf(File::exists)?.let { file ->
            runCatching { json.decodeFromString<DownloadRequest>(file.readText()) }.getOrNull()
        }

    fun delete(id: String) {
        fileFor(id).delete()
    }

    private fun fileFor(id: String): File = File(dir, "${id.toUrlSafeBase64()}.json")

    private fun String.toUrlSafeBase64(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))

    private companion object {
        const val STORE_DIR = "lamity_downloads"
    }
}
