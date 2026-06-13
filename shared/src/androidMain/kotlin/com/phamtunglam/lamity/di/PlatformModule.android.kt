package com.phamtunglam.lamity.di

import android.os.Build
import com.phamtunglam.lamity.core.platform.AndroidFileIo
import com.phamtunglam.lamity.core.platform.AppDirs
import com.phamtunglam.lamity.core.platform.FileIo
import com.phamtunglam.lamity.core.platform.PlatformInfo
import com.phamtunglam.lamity.db.LamityDatabase
import com.phamtunglam.lamity.db.lamityDatabaseBuilder
import com.phamtunglam.lamity.downloader.AndroidDownloader
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.llm.AndroidLlmBridge
import com.phamtunglam.lamity.llm.NativeLlmBridge
import androidx.room3.RoomDatabase
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<FileIo> { AndroidFileIo() }
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
    single<NativeLlmBridge> { AndroidLlmBridge() }
}
