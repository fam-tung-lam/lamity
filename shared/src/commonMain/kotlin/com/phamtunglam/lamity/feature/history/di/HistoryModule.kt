package com.phamtunglam.lamity.feature.history.di

import com.phamtunglam.lamity.feature.history.domain.DeleteConversationUseCase
import com.phamtunglam.lamity.feature.history.domain.ObserveConversationSummariesUseCase
import com.phamtunglam.lamity.feature.history.presentation.HistoryViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val historyModule: Module =
    module {
        // Domain
        factory { ObserveConversationSummariesUseCase(get(), get(), get()) }
        factory { DeleteConversationUseCase(get(), get()) }

        // Presentation
        viewModel { HistoryViewModel(get(), get(), get(), get()) }
    }
