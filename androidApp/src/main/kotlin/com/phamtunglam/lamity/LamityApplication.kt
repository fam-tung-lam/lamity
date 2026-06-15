package com.phamtunglam.lamity

import android.app.Application
import com.phamtunglam.lamity.core.di.appModule
import com.phamtunglam.lamity.core.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LamityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LamityApplication)
            modules(platformModule(), appModule)
        }
    }
}
