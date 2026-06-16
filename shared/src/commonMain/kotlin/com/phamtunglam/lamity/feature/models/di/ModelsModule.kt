package com.phamtunglam.lamity.feature.models.di

import com.phamtunglam.lamity.feature.models.data.ModelFiles
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.data.ModelsRepositoryImpl
import com.phamtunglam.lamity.feature.models.domain.CancelModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.DeleteModelFileUseCase
import com.phamtunglam.lamity.feature.models.domain.ObserveModelStatusesUseCase
import com.phamtunglam.lamity.feature.models.domain.ObserveModelsWithStatusUseCase
import com.phamtunglam.lamity.feature.models.domain.PauseModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.RemoveCustomModelUseCase
import com.phamtunglam.lamity.feature.models.domain.ResumeModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.SelectModelForChatUseCase
import com.phamtunglam.lamity.feature.models.domain.StartModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.presentation.ModelsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val modelsModule: Module =
    module {
        // Data
        single<ModelsRepository> { ModelsRepositoryImpl(get(), get()) }
        single { ModelFiles(get(), get()) }

        // Domain
        factory { ObserveModelStatusesUseCase(get(), get(), get()) }
        factory { ObserveModelsWithStatusUseCase(get(), get()) }
        factory { StartModelDownloadUseCase(get(), get(), get()) }
        factory { ResumeModelDownloadUseCase(get(), get()) }
        factory { PauseModelDownloadUseCase(get()) }
        factory { CancelModelDownloadUseCase(get()) }
        factory { DeleteModelFileUseCase(get()) }
        factory { SelectModelForChatUseCase(get()) }
        factory { RemoveCustomModelUseCase(get(), get(), get()) }

        // Presentation
        viewModel { ModelsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    }
