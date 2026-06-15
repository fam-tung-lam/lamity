package com.phamtunglam.lamity.feature.agents.di

import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.data.AgentsRepositoryImpl
import com.phamtunglam.lamity.feature.agents.domain.DeleteAgentUseCase
import com.phamtunglam.lamity.feature.agents.domain.SaveAgentUseCase
import com.phamtunglam.lamity.feature.agents.presentation.AgentEditViewModel
import com.phamtunglam.lamity.feature.agents.presentation.AgentsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val agentsModule: Module =
    module {
        // Data
        single<AgentsRepository> { AgentsRepositoryImpl(get(), get()) }

        // Domain
        factory { DeleteAgentUseCase(get(), get()) }
        factory { SaveAgentUseCase(get()) }

        // Presentation
        viewModel { AgentsViewModel(get(), get()) }
        viewModel { params ->
            AgentEditViewModel(params.getOrNull<String>(), get(), get(), get(), get(), get())
        }
    }
