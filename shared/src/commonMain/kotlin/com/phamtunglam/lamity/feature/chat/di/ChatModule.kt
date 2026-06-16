package com.phamtunglam.lamity.feature.chat.di

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepositoryImpl
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionFactory
import com.phamtunglam.lamity.feature.chat.domain.DeleteConversationUseCase
import com.phamtunglam.lamity.feature.chat.domain.LoadEngineUseCase
import com.phamtunglam.lamity.feature.chat.domain.ObserveConversationsUseCase
import com.phamtunglam.lamity.feature.chat.domain.tools.AppTool
import com.phamtunglam.lamity.feature.chat.domain.tools.CalculateTool
import com.phamtunglam.lamity.feature.chat.domain.tools.DeviceInfoTool
import com.phamtunglam.lamity.feature.chat.domain.tools.GetCurrentTimeTool
import com.phamtunglam.lamity.feature.chat.domain.tools.RandomNumberTool
import com.phamtunglam.lamity.feature.chat.domain.tools.SetLanguageTool
import com.phamtunglam.lamity.feature.chat.domain.tools.SetThemeTool
import com.phamtunglam.lamity.feature.chat.presentation.ChatViewModel
import com.phamtunglam.lamity.feature.chat.presentation.ChatsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatModule: Module =
    module {
        // Data
        single<ConversationsRepository> { ConversationsRepositoryImpl(get(), get()) }

        // The built-in tools, shared by the chat session and its settings sheet. load_skill is
        // created per chat session (it needs that session's skills), not listed here.
        single<List<AppTool>> {
            listOf(
                GetCurrentTimeTool(),
                CalculateTool(),
                SetThemeTool(get()),
                SetLanguageTool(get()),
                RandomNumberTool(),
                DeviceInfoTool(get()),
            )
        }

        // Domain
        factory { LoadEngineUseCase(get(), get(), get()) }
        factory { ChatSessionFactory(get()) }
        factory { ObserveConversationsUseCase(get()) }
        factory { DeleteConversationUseCase(get()) }

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
        viewModel { ChatsViewModel(get(), get(), get()) }
    }
