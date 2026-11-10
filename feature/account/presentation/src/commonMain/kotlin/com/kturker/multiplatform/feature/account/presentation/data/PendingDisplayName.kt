package com.kturker.multiplatform.feature.account.presentation.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * A sign-up's name that Firebase does not have yet, per user: [ProfileSync] puts it before writing it to Firebase
 * and clears it once Firebase has a name, so a write that failed is retried at that user's next sign-in.
 */
interface PendingDisplayName {
    suspend fun put(
        uid: String,
        name: String
    )

    suspend fun get(uid: String): String?

    suspend fun clear(uid: String)
}

/** Kept in DataStore: the app may be closed before the next sign-in. */
internal class DataStorePendingDisplayName(private val dataStore: DataStore<Preferences>) : PendingDisplayName {

    // One key per user: another account signing in on the same device must not get this user's name
    private fun key(uid: String) = stringPreferencesKey("pendingDisplayName_$uid")

    override suspend fun put(
        uid: String,
        name: String
    ) {
        dataStore.edit { it[key(uid)] = name }
    }

    override suspend fun get(uid: String): String? = dataStore.data.first()[key(uid)]

    override suspend fun clear(uid: String) {
        dataStore.edit { it.remove(key(uid)) }
    }
}
