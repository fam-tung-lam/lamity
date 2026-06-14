package com.phamtunglam.lamity.di

import com.phamtunglam.lamity.core.BuildType
import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.core.logging.CrashReportingLogWriter
import com.phamtunglam.lamity.core.tools.ToolContext
import com.phamtunglam.lamity.core.tools.ToolDispatcher
import com.phamtunglam.lamity.core.tools.ToolRegistry
import com.phamtunglam.lamity.crashreporter.LamityCrashReporter
import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
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
import com.phamtunglam.lamity.filesystem.LamityFileSystem
import com.phamtunglam.lamity.filesystem.lamityFileSystem
import com.phamtunglam.lamity.llm.ModelRuntime
import com.phamtunglam.lamity.logger.LamityLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Provided per platform: AppDirs, PlatformInfo, Downloader,
 * RoomDatabase.Builder<LamityDatabase> and NativeLlmBridge (Android only —
 * iOS injects the Swift bridge through
 * [com.phamtunglam.lamity.MainViewController]).
 *
 * [LamityFileSystem] is platform-backed too, but bound here in common via the
 * [lamityFileSystem] factory rather than per platform.
 */
expect fun platformModule(): Module

val appModule: Module = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // --------------------------------------------------------- file system
    single<LamityFileSystem> { lamityFileSystem() }

    // ------------------------------------------------------------- logging
    single(createdAtStart = true) {
        // Initializes crash reporting from platform build metadata and routes app error
        // logs into it as captures by registering the bridge writer with the logging
        // facade. Skipped entirely when no Sentry DSN is configured (e.g. local builds
        // without a SENTRY_DSN secret), leaving crash reporting disabled.
        val dsn = LamityBuildConfig.sentryDsn
        if (dsn.isNotBlank()) {
            LamityCrashReporter.init(
                LamityCrashReporterConfig(
                    dsn = dsn,
                    environment = LamityBuildConfig.buildType.environmentName,
                    release = "${LamityBuildConfig.packageName}@${LamityBuildConfig.appVersion}+${LamityBuildConfig.appVersionCode}",
                    debug = LamityBuildConfig.buildType == BuildType.DEBUG,
                ),
            )
            LamityLogger.addWriter(CrashReportingLogWriter(reporter = LamityCrashReporter))
        }
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
            fileSystem = get(),
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
    factory { ObserveModelsWithStatusUseCase(get(), get()) }
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
