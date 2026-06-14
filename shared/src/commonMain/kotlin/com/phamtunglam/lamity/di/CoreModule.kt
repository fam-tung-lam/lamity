package com.phamtunglam.lamity.di

import com.phamtunglam.lamity.core.BuildType
import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.core.logging.CrashReportingLogWriter
import com.phamtunglam.lamity.crashreporter.LamityCrashReporter
import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
import com.phamtunglam.lamity.filesystem.LamityFileSystem
import com.phamtunglam.lamity.filesystem.lamityFileSystem
import com.phamtunglam.lamity.logger.LamityLogger
import com.phamtunglam.lamity.logger.logWriters.PlatformLamityLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // File system
    single<LamityFileSystem> { lamityFileSystem() }

    // Logging
    single(createdAtStart = true) {
        LamityLogger.addWriter(PlatformLamityLogWriter)

        // Initializes crash reporting and routes app error logs into it as
        // captures by registering the bridge writer with the logging facade.
        if (LamityBuildConfig.sentryDsn.isNotBlank()) {
            LamityCrashReporter.init(
                LamityCrashReporterConfig(
                    dsn = LamityBuildConfig.sentryDsn,
                    environment = LamityBuildConfig.buildType.environmentName,
                    release = "${LamityBuildConfig.packageName}@${LamityBuildConfig.appVersion}+${LamityBuildConfig.appVersionCode}",
                    debug = LamityBuildConfig.buildType == BuildType.DEBUG,
                ),
            )
            LamityLogger.addWriter(CrashReportingLogWriter(reporter = LamityCrashReporter))
        }
    }
}
