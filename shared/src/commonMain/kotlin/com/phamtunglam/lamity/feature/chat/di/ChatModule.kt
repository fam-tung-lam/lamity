package com.phamtunglam.lamity.feature.chat.di

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepositoryImpl
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager
import com.phamtunglam.lamity.feature.chat.presentation.ChatViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatModule: Module = module {
    // Data
    single<ConversationsRepository> { ConversationsRepositoryImpl(get(), get()) }

    // ----------------------------------------------------------------- llm
    single {
        ChatSessionManager(
            scope = get(),
            runtime = get(),
            conversations = get(),
            agents = get(),
            skills = get(),
            models = get(),
            settings = get(),
            registry = get(),
            toolContext = get(),
            dispatcher = get(),
            downloads = get(),
            dirs = get(),
            llmBridge = get(),
        )
    }

    // Presentation
    viewModel { ChatViewModel(get(), get(), get(), get()) }
}
