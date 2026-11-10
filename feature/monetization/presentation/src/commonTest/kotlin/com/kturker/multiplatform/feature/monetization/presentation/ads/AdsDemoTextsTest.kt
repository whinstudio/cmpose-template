package com.kturker.multiplatform.feature.monetization.presentation.ads

import kotlin.test.Test
import kotlin.test.assertEquals

class AdsDemoTextsTest {

    @Test
    fun adFailedShowsTheSdksReason() {
        assertEquals("Ad failed: no fill", AdsDemoTexts().message(AdsMessage.AdFailed("no fill")))
    }

    @Test
    fun coinsLabelShowsTheCount() {
        assertEquals("Coins: 3", AdsDemoTexts().coinsLabel(3))
    }
}
