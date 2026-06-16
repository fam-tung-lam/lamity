package com.phamtunglam.lamity.downloader

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.phamtunglam.lamity.downloader.models.DownloadException
import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.downloader.persistence.RequestStore
import com.phamtunglam.lamity.downloader.workmanager.DownloadWorkData
import com.phamtunglam.lamity.downloader.workmanager.DownloadWorker
import com.phamtunglam.lamity.downloader.workmanager.WorkInfoMapping
import com.phamtunglam.lamity.downloader.workmanager.partialFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okio.FileSystem

/**
 * [Downloader] backed by WorkManager: one unique work chain per download id,
 * surviving process death. The full [DownloadRequest] is persisted by
 * [RequestStore] (WorkManager `Data` is too small for it), pause keeps the
 * partial file for an HTTP-Range resume, and progress reaches observers
 * through WorkManager progress data.
 */
class AndroidDownloader internal constructor(
    private val workManager: WorkManager,
    private val store: RequestStore,
    private val fileSystem: FileSystem,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Downloader {
    companion object {
        /** Wires the production WorkManager-backed downloader from a [Context]. */
        fun create(context: Context): AndroidDownloader {
            val appContext = context.applicationContext
            return AndroidDownloader(
                workManager = WorkManager.getInstance(appContext),
                store = RequestStore(appContext),
                fileSystem = FileSystem.SYSTEM,
            )
        }
    }

    override suspend fun start(request: DownloadRequest) {
        store.save(request)
        workManager
            .enqueueUniqueWork(request.id, ExistingWorkPolicy.REPLACE, workRequest(request))
            .await()
    }

    override suspend fun pause(id: String) {
        workManager.cancelUniqueWork(id).await()
    }

    override suspend fun resume(id: String) {
        val request =
            store.load(id)
                ?: throw DownloadException("No stored download request for '$id'.")
        start(request)
    }

    override suspend fun cancel(id: String) {
        val request = store.load(id)
        workManager.cancelUniqueWork(id).await()
        request?.partialFile()?.let { fileSystem.delete(it, mustExist = false) }
        store.delete(id)
    }

    override fun observe(id: String): Flow<DownloadProgress?> =
        workManager
            .getWorkInfosForUniqueWorkFlow(id)
            .map { workInfos ->
                val request = store.load(id)
                WorkInfoMapping.toProgress(
                    id = id,
                    workInfo = workInfos.firstOrNull(),
                    partialBytes = request?.partialFile()?.let { fileSystem.metadataOrNull(it)?.size } ?: 0L,
                    totalBytes = request?.expectedSizeBytes ?: 0L,
                )
            }.distinctUntilChanged()
            .flowOn(dispatcher)

    private fun workRequest(request: DownloadRequest): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorkData.Keys.ID to request.id))
            .setConstraints(
                Constraints(
                    requiredNetworkType =
                        if (request.requireUnmetered) {
                            NetworkType.UNMETERED
                        } else {
                            NetworkType.CONNECTED
                        },
                ),
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(DownloadWorkData.Tags.ALL)
            .build()
}
