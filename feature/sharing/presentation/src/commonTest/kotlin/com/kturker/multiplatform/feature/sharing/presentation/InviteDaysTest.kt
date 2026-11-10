package com.kturker.multiplatform.feature.sharing.presentation

import com.kturker.multiplatform.feature.sharing.presentation.ui.members.daysLeft
import kotlin.test.Test
import kotlin.test.assertEquals

class InviteDaysTest {

    // Any part of a day left counts as a whole day; an invite never shows zero days
    @Test
    fun daysLeftRoundsUpToWholeDays() {
        val now = 1_000_000L
        val day = 86_400_000L

        assertEquals(1, daysLeft(now + 1, now))
        assertEquals(7, daysLeft(now + day * 7, now))
        assertEquals(7, daysLeft(now + day * 6 + 1, now))
        assertEquals(1, daysLeft(now, now))
    }
}
