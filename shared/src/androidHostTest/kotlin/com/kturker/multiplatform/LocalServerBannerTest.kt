package com.kturker.multiplatform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalServerBannerTest {

    @Test
    fun theOfflineStripWins() {
        assertTrue(showsLocalServerBanner(unreachable = true, online = true))
        assertFalse(showsLocalServerBanner(unreachable = true, online = false))
        assertFalse(showsLocalServerBanner(unreachable = false, online = true))
        assertFalse(showsLocalServerBanner(unreachable = false, online = false))
    }
}
