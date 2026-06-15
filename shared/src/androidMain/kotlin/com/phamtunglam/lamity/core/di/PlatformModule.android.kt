package com.phamtunglam.lamity.core.di

import android.os.Build
import androidx.room3.RoomDatabase
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.core.domain.platform.PlatformInfo
import com.phamtunglam.lamity.db.LamityDatabase
import com.phamtunglam.lamity.db.lamityDatabaseBuilder
import com.phamtunglam.lamity.downloader.AndroidDownloader
import com.phamtunglam.lamity.downloader.Downloader
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module =
    module {
        single {
            val context = androidContext()
            AppDirs(
                dataDir = File(context.filesDir, "appdata").absolutePath,
                modelsDir = File(context.filesDir, "models").absolutePath,
                cacheDir = context.cacheDir.absolutePath,
            )
        }
        single {
            PlatformInfo(
                platform = "Android",
                osVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            )
        }
        single<Downloader> { AndroidDownloader(androidContext()) }
        single<RoomDatabase.Builder<LamityDatabase>> { lamityDatabaseBuilder(androidContext()) }
    }
