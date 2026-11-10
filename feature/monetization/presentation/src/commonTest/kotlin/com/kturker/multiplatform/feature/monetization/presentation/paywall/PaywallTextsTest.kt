package com.kturker.multiplatform.feature.monetization.presentation.paywall

import com.kturker.multiplatform.core.purchases.PackagePeriod
import com.kturker.multiplatform.core.purchases.PurchaseFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class PaywallTextsTest {

    private val texts = PaywallTexts()

    @Test
    fun everyFailureHasItsOwnMessage() {
        val messages = PurchaseFailure.entries.map { texts.message(PaywallError.Purchase(it)) }

        assertEquals(PurchaseFailure.entries.size, messages.toSet().size)
    }

    @Test
    fun plansUnavailableIsNotAPurchaseMessage() {
        assertEquals(texts.plansUnavailable, texts.message(PaywallError.PlansUnavailable))
    }

    @Test
    fun pricesKeepTheStoresFormat() {
        assertEquals("$29.99 / year", texts.priceLabel("$29.99", PackagePeriod.ANNUAL))
        assertEquals("$1", texts.priceLabel("$1", PackagePeriod.OTHER))
        assertEquals("Save 17%", texts.saveLabel(17))
    }
}
