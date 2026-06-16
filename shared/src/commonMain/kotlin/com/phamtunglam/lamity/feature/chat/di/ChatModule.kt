package com.phamtunglam.lamity.feature.chat.di

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepositoryImpl
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionFactory
import com.phamtunglam.lamity.feature.chat.domain.LoadEngineUseCase
import com.phamtunglam.lamity.feature.chat.presentation.ChatViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatModule: Module =
    module {
        // Data
        single<ConversationsRepository> { ConversationsRepositoryImpl(get(), get()) }

        // Domain
        factory { LoadEngineUseCase(get(), get(), get()) }
        factory { ChatSessionFactory(get()) }

        // Presentation
        viewModel { params ->
            ChatViewModel(
                conversationId = params.getOrNull<String>(),
                runtime = get(),
                conversations = get(),
                models = get(),
                settings = get(),
                modelFiles = get(),
                loadEngine = get(),
                sessionFactory = get(),
                tools = get(),
                observeStatuses = get(),
            )
        }
    }
