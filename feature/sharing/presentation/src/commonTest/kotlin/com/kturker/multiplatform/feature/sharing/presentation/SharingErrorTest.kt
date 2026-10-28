package com.kturker.multiplatform.feature.sharing.presentation

import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError
import com.kturker.multiplatform.feature.sharing.presentation.ui.retryable
import kotlin.test.Test
import kotlin.test.assertEquals

class SharingErrorTest {

    // Asking again cannot get past a quota (the user has to delete a space or revoke a link first)
    @Test
    fun onlyTheQuotaErrorsAreNotRetryable() {
        assertEquals(
            listOf(SharingError.SPACE_LIMIT, SharingError.INVITE_LIMIT),
            SharingError.entries.filterNot { it.retryable }
        )
    }
}
