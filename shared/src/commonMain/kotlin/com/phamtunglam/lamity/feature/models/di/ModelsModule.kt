package com.phamtunglam.lamity.feature.models.di

import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.data.ModelsRepositoryImpl
import com.phamtunglam.lamity.feature.models.domain.ObserveModelsWithStatusUseCase
import com.phamtunglam.lamity.feature.models.domain.RemoveCustomModelUseCase
import com.phamtunglam.lamity.feature.models.domain.SaveModelConfigUseCase
import com.phamtunglam.lamity.feature.models.presentation.ModelConfigViewModel
import com.phamtunglam.lamity.feature.models.presentation.ModelsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val modelsModule: Module = module {
    // Data
    single<ModelsRepository> { ModelsRepositoryImpl(get(), get()) }
    single {
        ModelDownloadManager(
            dirs = get(),
            fileSystem = get(),
            settings = get(),
            downloader = get(),
            models = get(),
            scope = get(),
        )
    }

    // Domain
    factory { ObserveModelsWithStatusUseCase(get(), get()) }
    factory { RemoveCustomModelUseCase(get(), get()) }
    factory { SaveModelConfigUseCase(get()) }

    // Presentation
    viewModel { ModelsViewModel(get(), get(), get(), get(), get()) }
    viewModel { params -> ModelConfigViewModel(params.get<String>(), get(), get()) }
}
