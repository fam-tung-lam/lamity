package com.phamtunglam.lamity.di

import com.phamtunglam.lamity.core.platform.AppDirs
import com.phamtunglam.lamity.core.platform.FileIo
import com.phamtunglam.lamity.core.platform.IosFileIo
import com.phamtunglam.lamity.core.platform.PlatformInfo
import com.phamtunglam.lamity.db.LamityDatabase
import com.phamtunglam.lamity.db.lamityDatabaseBuilder
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.iosDownloader
import androidx.room3.RoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

/**
 * iOS platform graph. NativeLlmBridge and CrashReporter are NOT defined here:
 * the Swift side owns those SDKs and hands its implementations to
 * [com.phamtunglam.lamity.MainViewController], which registers them in Koin.
 */
actual fun platformModule(): Module = module {
    single<FileIo> { IosFileIo() }
    single {
        val documents = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true,
        ).firstOrNull() as? String ?: NSTemporaryDirectory()
        val caches = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory, NSUserDomainMask, true,
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
