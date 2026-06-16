package com.phamtunglam.lamity.feature.llmModels.di

import com.phamtunglam.lamity.feature.llmModels.data.ModelFiles
import com.phamtunglam.lamity.feature.llmModels.data.ModelsRepository
import com.phamtunglam.lamity.feature.llmModels.data.ModelsRepositoryImpl
import com.phamtunglam.lamity.feature.llmModels.domain.CancelModelDownloadUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.DeleteModelFileUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.ObserveModelStatusesUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.ObserveModelsWithStatusUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.PauseModelDownloadUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.ResumeModelDownloadUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.SelectModelForChatUseCase
import com.phamtunglam.lamity.feature.llmModels.domain.StartModelDownloadUseCase
import com.phamtunglam.lamity.feature.llmModels.presentation.ModelsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val modelsModule: Module =
    module {
        // Data
        single<ModelsRepository> { ModelsRepositoryImpl() }
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

        // Presentation
        viewModel { ModelsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    }
