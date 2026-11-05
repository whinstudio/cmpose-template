package com.kturker.multiplatform.core.database.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import okio.Path.Companion.toPath

expect fun getPlatformDataStore(): DataStore<Preferences>

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        // Without it a corrupt file throws on every read, so the app crashes on every launch.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        produceFile = { producePath().toPath() }
    )

internal const val DATA_STORE_FILE_NAME = "multiplatform.preferences_pb"
