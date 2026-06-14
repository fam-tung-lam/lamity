package com.phamtunglam.lamity.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.FileSystem
import okio.Path.Companion.toPath

/** File name for the app-wide preferences DataStore. */
const val DATASTORE_FILE_NAME = "lamity.preferences_pb"

/**
 * Builds the process-wide preferences [DataStore], persisted as
 * [DATASTORE_FILE_NAME] inside [dataDir]. The directory is created if missing.
 *
 * Preferences DataStore requires a single instance per file for the whole
 * process, so this must be provided as a singleton.
 */
fun createPreferenceDataStore(dataDir: String): DataStore<Preferences> {
    val directory = dataDir.toPath()
    FileSystem.SYSTEM.createDirectories(directory)
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { directory.resolve(DATASTORE_FILE_NAME) },
    )
}
