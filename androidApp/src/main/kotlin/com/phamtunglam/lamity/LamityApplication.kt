package com.phamtunglam.lamity

import android.app.Application
import com.phamtunglam.lamity.di.appModule
import com.phamtunglam.lamity.di.platformModule
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
