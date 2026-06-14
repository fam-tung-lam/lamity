package com.phamtunglam.lamity.feature.localization.data

import android.content.res.Resources

actual fun systemDeviceLocaleProvider(): DeviceLocaleProvider =
    DeviceLocaleProvider {
        Resources
            .getSystem()
            .configuration.locales[0]
            ?.toLanguageTag()
    }
