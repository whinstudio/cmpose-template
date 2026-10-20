package com.kturker.multiplatform.feature.sharing.presentation

import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.feature.sharing.presentation.data.PersistedSelectedSpace
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectedSpaceTest {

    private val dataStore = InMemoryPreferences()
    private val repository = FakeSpacesRepository()

    private val personal = space("personal", personal = true)
    private val home = space("home", role = Role.EDITOR, memberCount = 2)

    private fun selectedSpace() = PersistedSelectedSpace(repository, dataStore)

    @Test
    fun nothingIsSelectedBeforeTheFirstRefresh() {
        assertNull(selectedSpace().current.value)
    }

    @Test
    fun withoutAChoiceThePersonalSpaceIsSelected() = runTest {
        repository.listResult = RestResult.Success(listOf(home, personal))
        val selected = selectedSpace()

        selected.refresh()

        assertEquals(personal, selected.current.value)
    }

    @Test
    fun aSelectedSpaceIsKeptAcrossRestarts() = runTest {
        repository.listResult = RestResult.Success(listOf(personal, home))
        selectedSpace().select("home")

        val afterRestart = selectedSpace()
        afterRestart.refresh()

        assertEquals(home, afterRestart.current.value)
    }

    @Test
    fun selectingShowsTheSpaceAtOnce() = runTest {
        repository.listResult = RestResult.Success(listOf(personal, home))
        val selected = selectedSpace()
        selected.refresh()

        selected.select("home")

        assertEquals(home, selected.current.value)
    }

    // Removed from the space, or the owner deleted it: the next refresh falls back to the personal space
    @Test
    fun aSpaceThatLeftTheListFallsBackToPersonal() = runTest {
        repository.listResult = RestResult.Success(listOf(personal, home))
        val selected = selectedSpace()
        selected.select("home")
        assertEquals(home, selected.current.value)

        repository.listResult = RestResult.Success(listOf(personal))
        selected.refresh()

        assertEquals(personal, selected.current.value)
        // And it is remembered: the space is not chosen again should the user be invited back later
        repository.listResult = RestResult.Success(listOf(personal, home))
        selected.refresh()
        assertEquals(personal, selected.current.value)
    }

    @Test
    fun aRenamedSpaceShowsItsNewName() = runTest {
        repository.listResult = RestResult.Success(listOf(personal, home))
        val selected = selectedSpace()
        selected.select("home")

        repository.listResult = RestResult.Success(listOf(personal, home.copy(name = "Our home")))
        selected.refresh()

        assertEquals("Our home", selected.current.value?.name)
    }

    // Offline: the screens keep showing the space they had
    @Test
    fun aFailedRefreshKeepsTheSelection() = runTest {
        repository.listResult = RestResult.Success(listOf(personal, home))
        val selected = selectedSpace()
        selected.select("home")

        repository.listResult = RestResult.Error(DataError.Network)
        selected.refresh()

        assertEquals(home, selected.current.value)
    }

    // A sign-out or another user: the previous user's space is neither shown nor stored for the next one
    @Test
    fun clearingForgetsTheShownAndTheStoredSpace() = runTest {
        repository.listResult = RestResult.Success(listOf(personal, home))
        val selected = selectedSpace()
        selected.select("home")

        selected.clear()

        assertNull(selected.current.value)
        val nextUser = selectedSpace()
        nextUser.refresh()
        assertEquals(personal, nextUser.current.value)
    }
}
