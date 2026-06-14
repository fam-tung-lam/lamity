package com.phamtunglam.lamity.feature.localization.data

import com.phamtunglam.lamity.feature.localization.domain.AppLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Resolves the app locale from stored preference, device locale, then English. */
class AppLocaleRepository(store: AppLocaleStore, private val deviceLocaleProvider: DeviceLocaleProvider) {
    /** Emits the concrete locale used by app resources. */
    val locale: Flow<AppLocale> =
        store.locale.map { storedLocale ->
            storedLocale
                ?: AppLocale.fromDeviceLocaleTag(deviceLocaleProvider.currentLocaleTag())
                ?: AppLocale.ENGLISH
        }
}
