package com.kturker.multiplatform.feature.sharing.presentation.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [SelectedSpace] with the chosen space's id in DataStore. Each [refresh] reads GET spaces and shows the stored
 * space while the user is still a member of it, otherwise the personal space, which it then stores. A failed
 * refresh (offline) keeps the space shown.
 */
internal class PersistedSelectedSpace(
    private val repository: SpacesRepository,
    private val dataStore: DataStore<Preferences>
) : SelectedSpace {

    private val key = stringPreferencesKey("selectedSpace")
    private val _current = MutableStateFlow<Space?>(null)
    override val current: StateFlow<Space?> = _current.asStateFlow()

    // A select and a refresh at once: one finishes before the other reads the stored id
    private val mutex = Mutex()

    override suspend fun select(spaceId: String) = mutex.withLock {
        dataStore.edit { it[key] = spaceId }
        // The list is read again: a space just joined through an invite is not in the last one
        load()
    }

    override suspend fun refresh() = mutex.withLock { load() }

    // The next user starts from their own personal space, not from the id the previous one chose
    override suspend fun clear() = mutex.withLock {
        dataStore.edit { it.remove(key) }
        _current.value = null
    }

    private suspend fun load() {
        val spaces = (repository.list() as? RestResult.Success)?.result ?: return
        val stored = dataStore.data.first()[key]
        val chosen = spaces.firstOrNull { it.id == stored } ?: spaces.firstOrNull { it.personal }
        if (chosen != null && chosen.id != stored) dataStore.edit { it[key] = chosen.id }
        _current.value = chosen
    }
}
