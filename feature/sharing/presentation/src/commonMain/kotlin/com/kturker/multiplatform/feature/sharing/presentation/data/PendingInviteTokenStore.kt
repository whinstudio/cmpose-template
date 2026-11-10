package com.kturker.multiplatform.feature.sharing.presentation.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * An invite link opened while nobody was signed in: the invite screen puts its token here and opens the sign-in;
 * after the sign-in the app shell takes it and opens the invite again.
 */
interface PendingInviteTokenStore {
    /** Replaces any waiting token. */
    suspend fun put(token: String)

    /** The waiting token, once: the call that returns it also clears it. */
    suspend fun take(): String?
}

/** Kept in DataStore: the app may be closed between opening the link and signing in. */
internal class DataStorePendingInviteTokenStore(private val dataStore: DataStore<Preferences>) :
    PendingInviteTokenStore {

    // The key keeps its old name: a token waiting across an app update is still found
    private val key = stringPreferencesKey("pendingInvite")

    override suspend fun put(token: String) {
        dataStore.edit { it[key] = token }
    }

    override suspend fun take(): String? {
        var token: String? = null
        // Read and cleared in one edit: two callers never both get the token
        dataStore.edit { preferences ->
            token = preferences[key]
            preferences.remove(key)
        }
        return token
    }
}
