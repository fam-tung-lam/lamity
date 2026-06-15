package com.phamtunglam.lamity.core.di

import androidx.room3.RoomDatabase
import com.phamtunglam.lamity.core.data.db.LamityDatabase
import com.phamtunglam.lamity.core.data.db.lamityDatabaseBuilder
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.core.domain.platform.PlatformInfo
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.iosDownloader
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

/**
 * iOS platform graph. The LiteRT-LM runtime lives in lamityLlm's iosMain (cinterop to the
 * CLiteRTLM C API), so no Swift LLM bridge is registered here.
 */
actual fun platformModule(): Module =
    module {
        single {
            val documents =
                NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true,
                ).firstOrNull() as? String ?: NSTemporaryDirectory()
            val caches =
                NSSearchPathForDirectoriesInDomains(
                    NSCachesDirectory,
                    NSUserDomainMask,
                    true,
                ).firstOrNull() as? String ?: NSTemporaryDirectory()
            AppDirs(
                dataDir = "$documents/appdata",
                modelsDir = "$documents/models",
                cacheDir = caches,
            )
        }
        single {
            PlatformInfo(
                platform = UIDevice.currentDevice.systemName(),
                osVersion = UIDevice.currentDevice.systemVersion,
                deviceModel = UIDevice.currentDevice.model,
            )
        }
        single<Downloader> { iosDownloader() }
        single<RoomDatabase.Builder<LamityDatabase>> { lamityDatabaseBuilder() }
    }
