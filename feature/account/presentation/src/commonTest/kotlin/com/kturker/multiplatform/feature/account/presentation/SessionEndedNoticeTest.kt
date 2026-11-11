package com.kturker.multiplatform.feature.account.presentation

import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.feature.account.presentation.ui.login.LoginViewModel
import com.kturker.multiplatform.feature.account.presentation.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A 401 the token refresh could not cure signs the user out; the sign-in screen that opens next says the
 * session ended, once. A sign-out the user chose opens the sign-in screen without that notice.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionEndedNoticeTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService(log)
    private val repository = FakeAccountRepository(log)
    private val tokens = FakeSessionTokens(log)
    private val profileSync = FakeProfileSync()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun settings() =
        SettingsViewModel(repository, auth, tokens, RecordingNavigationManager(), FakePendingDisplayName(), FakeAppInfo())

    @Test
    fun aSignOutOnUnauthorizedShowsTheNoticeOnce() = runTest {
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(401))
        settings().apply {
            requestDelete()
            // Delete waits for the shared-space count
            advanceUntilIdle()
            confirmDelete()
        }
        advanceUntilIdle()

        assertTrue(LoginViewModel(auth, profileSync, FakeAppInfo()).state.value.sessionEnded)
        // A later sign-in screen no longer shows it
        assertFalse(LoginViewModel(auth, profileSync, FakeAppInfo()).state.value.sessionEnded)
    }

    @Test
    fun aSignOutTheUserChoseShowsNoNotice() = runTest {
        settings().signOut()
        advanceUntilIdle()

        assertFalse(LoginViewModel(auth, profileSync, FakeAppInfo()).state.value.sessionEnded)
    }

    @Test
    fun aSignInAttemptClearsTheNotice() = runTest {
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(401))
        settings().apply {
            requestDelete()
            // Delete waits for the shared-space count
            advanceUntilIdle()
            confirmDelete()
        }
        advanceUntilIdle()
        val login = LoginViewModel(auth, profileSync, FakeAppInfo())

        login.signInWithGoogle()

        assertFalse(login.state.value.sessionEnded)
    }
}
