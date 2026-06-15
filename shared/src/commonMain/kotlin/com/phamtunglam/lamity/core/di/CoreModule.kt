package com.phamtunglam.lamity.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.phamtunglam.lamity.core.BuildType
import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.core.data.createPreferenceDataStore
import com.phamtunglam.lamity.core.data.logging.CrashReportingLogWriter
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.crashreporter.LamityCrashReporter
import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
import com.phamtunglam.lamity.logger.LamityLogger
import com.phamtunglam.lamity.logger.logWriters.PlatformLamityLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module =
    module {

        single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

        // File system
        single<FileSystem> { FileSystem.SYSTEM }

        // App-wide preferences DataStore (backs settings and localization)
        single<DataStore<Preferences>> { createPreferenceDataStore(get<AppDirs>().dataDir) }

        // Logging
        single(createdAtStart = true) {
            LamityLogger.addWriter(PlatformLamityLogWriter)

            // Initializes crash reporting and routes app error logs into it as
            // captures by registering the bridge writer with the logging facade.
            if (LamityBuildConfig.sentryDsn.isNotBlank()) {
                val release =
                    "${LamityBuildConfig.packageName}@${LamityBuildConfig.appVersion}" +
                        "+${LamityBuildConfig.appVersionCode}"
                LamityCrashReporter.init(
                    LamityCrashReporterConfig(
                        dsn = LamityBuildConfig.sentryDsn,
                        environment = LamityBuildConfig.buildType.environmentName,
                        release = release,
                        debug = LamityBuildConfig.buildType == BuildType.DEBUG,
                    ),
                )
                LamityLogger.addWriter(CrashReportingLogWriter(reporter = LamityCrashReporter))
            }
        }
    }
