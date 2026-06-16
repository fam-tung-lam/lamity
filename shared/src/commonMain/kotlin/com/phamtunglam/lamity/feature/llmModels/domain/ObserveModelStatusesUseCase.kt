package com.phamtunglam.lamity.feature.llmModels.domain

import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.feature.llmModels.data.ModelFiles
import com.phamtunglam.lamity.feature.llmModels.data.ModelStatusMapper
import com.phamtunglam.lamity.feature.llmModels.data.ModelsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Streams the per-model download [ModelStatus] map by folding the background [Downloader]'s progress
 * with on-disk presence. [refresh] re-checks on-disk state after changes the downloader does not emit
 * for (e.g. a local file delete); pass `flowOf(Unit)` when no manual refresh is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveModelStatusesUseCase(
    private val models: ModelsRepository,
    private val downloader: Downloader,
    private val modelFiles: ModelFiles,
) {
    operator fun invoke(refresh: Flow<Unit>): Flow<Map<String, ModelStatus>> =
        combine(models.models, refresh) { list, _ -> list }
            .flatMapLatest { list -> statusesOf(list) }

    private fun statusesOf(models: List<LlmModel>): Flow<Map<String, ModelStatus>> =
        if (models.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                models.map { model ->
                    downloader
                        .observe(model.id)
                        .onStart { emit(null) }
                        .map { progress -> model.id to ModelStatusMapper.map(progress, modelFiles.isDownloaded(model)) }
                },
            ) { entries -> entries.toMap() }
        }
}
