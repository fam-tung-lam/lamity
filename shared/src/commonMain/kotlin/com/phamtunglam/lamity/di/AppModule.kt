package com.phamtunglam.lamity.di

import com.phamtunglam.lamity.LamityConfig
import com.phamtunglam.lamity.core.tools.ToolContext
import com.phamtunglam.lamity.core.tools.ToolDispatcher
import com.phamtunglam.lamity.core.tools.ToolRegistry
import com.phamtunglam.lamity.crashreporter.CrashReporter
import com.phamtunglam.lamity.crashreporter.attachToLogger
import com.phamtunglam.lamity.crashreporter.sentryCrashReporter
import com.phamtunglam.lamity.db.LamityDatabase
import com.phamtunglam.lamity.db.buildLamityDatabase
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepositoryImpl
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager
import com.phamtunglam.lamity.feature.chat.presentation.ChatViewModel
import com.phamtunglam.lamity.feature.history.domain.DeleteConversationUseCase
import com.phamtunglam.lamity.feature.history.domain.ObserveConversationSummariesUseCase
import com.phamtunglam.lamity.feature.history.presentation.HistoryViewModel
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.data.ModelsRepositoryImpl
import com.phamtunglam.lamity.feature.models.domain.ObserveModelsWithStatusUseCase
import com.phamtunglam.lamity.feature.models.domain.RemoveCustomModelUseCase
import com.phamtunglam.lamity.feature.models.domain.SaveModelConfigUseCase
import com.phamtunglam.lamity.feature.models.presentation.ModelConfigViewModel
import com.phamtunglam.lamity.feature.models.presentation.ModelsViewModel
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepositoryImpl
import com.phamtunglam.lamity.feature.settings.presentation.SettingsViewModel
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.data.AgentsRepositoryImpl
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.feature.studio.data.SkillsRepositoryImpl
import com.phamtunglam.lamity.feature.studio.domain.DeleteAgentUseCase
import com.phamtunglam.lamity.feature.studio.domain.DeleteSkillUseCase
import com.phamtunglam.lamity.feature.studio.domain.SaveAgentUseCase
import com.phamtunglam.lamity.feature.studio.domain.SaveSkillUseCase
import com.phamtunglam.lamity.feature.studio.presentation.AgentEditViewModel
import com.phamtunglam.lamity.feature.studio.presentation.SkillEditViewModel
import com.phamtunglam.lamity.feature.studio.presentation.StudioViewModel
import com.phamtunglam.lamity.llm.ModelRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Provided per platform: FileIo, AppDirs, PlatformInfo, FileDownloader,
 * RoomDatabase.Builder<LamityDatabase> and NativeLlmBridge (Android only —
 * iOS injects the Swift bridge through
 * [com.phamtunglam.lamity.MainViewController]).
 */
expect fun platformModule(): Module

val appModule: Module = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<CrashReporter>(createdAtStart = true) {
        sentryCrashReporter(LamityConfig.crashReporterConfig()).also { it.attachToLogger() }
    }

    // ------------------------------------------------------------ database
    single<LamityDatabase> { buildLamityDatabase(get()) }
    single { get<LamityDatabase>().settingsDao() }
    single { get<LamityDatabase>().modelsDao() }
    single { get<LamityDatabase>().agentsDao() }
    single { get<LamityDatabase>().skillsDao() }
    single { get<LamityDatabase>().conversationsDao() }

    // ---------------------------------------------------------------- data
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }
    single<ModelsRepository> { ModelsRepositoryImpl(get(), get()) }
    single<AgentsRepository> { AgentsRepositoryImpl(get(), get()) }
    single<SkillsRepository> { SkillsRepositoryImpl(get(), get()) }
    single<ConversationsRepository> { ConversationsRepositoryImpl(get(), get()) }
    single {
        ModelDownloadManager(
            dirs = get(),
            fileIo = get(),
            settings = get(),
            downloader = get(),
            models = get(),
            scope = get(),
        )
    }

    // --------------------------------------------------------------- tools
    single { ToolContext(get(), get(), get()) }
    single { ToolRegistry(get()) }
    single { ToolDispatcher(get()) }

    // ----------------------------------------------------------------- llm
    single { ModelRuntime(get()) }
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

    // -------------------------------------------------------------- domain
    factory { ObserveConversationSummariesUseCase(get(), get(), get()) }
    factory { DeleteConversationUseCase(get(), get()) }
    factory { ObserveModelsWithStatusUseCase(get(), get(), get()) }
    factory { RemoveCustomModelUseCase(get(), get()) }
    factory { SaveModelConfigUseCase(get()) }
    factory { DeleteAgentUseCase(get(), get()) }
    factory { DeleteSkillUseCase(get(), get()) }
    factory { SaveAgentUseCase(get()) }
    factory { SaveSkillUseCase(get()) }

    // ------------------------------------------------------------ presentation
    viewModel { ChatViewModel(get(), get(), get(), get()) }
    viewModel { ModelsViewModel(get(), get(), get(), get(), get()) }
    viewModel { params -> ModelConfigViewModel(params.get<String>(), get(), get()) }
    viewModel { HistoryViewModel(get(), get(), get(), get()) }
    viewModel { StudioViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { params ->
        AgentEditViewModel(params.getOrNull<String>(), get(), get(), get(), get())
    }
    viewModel { params -> SkillEditViewModel(params.getOrNull<String>(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
