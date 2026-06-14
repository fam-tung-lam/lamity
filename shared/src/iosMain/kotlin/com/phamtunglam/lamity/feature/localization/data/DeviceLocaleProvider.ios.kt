package com.phamtunglam.lamity.feature.localization.data

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun systemDeviceLocaleProvider(): DeviceLocaleProvider =
    DeviceLocaleProvider { NSLocale.preferredLanguages.firstOrNull() as? String }
