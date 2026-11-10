package com.kturker.multiplatform.core.purchases

import com.kturker.multiplatform.core.domain.BuildEnvironment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// A release build with the shipped Test Store key: RevenueCat is never configured, so none of these may reach the SDK
class RevenueCatPurchasesManagerTest {

    private val manager = RevenueCatPurchasesManager(environment = BuildEnvironment(isDebug = false))

    @Test
    fun releaseBuildsWithATestStoreKeyHaveNoRevenueCat() {
        assertFalse(manager.isAvailable)
    }

    @Test
    fun withoutRevenueCatThereIsNoOffer() = runTest {
        assertNull(manager.offer())
    }

    @Test
    fun withoutRevenueCatAPurchaseIsAnError() = runTest {
        assertEquals(PurchaseResult.Error(PurchaseFailure.PURCHASES_UNAVAILABLE), manager.purchase(packageId = "annual"))
    }

    @Test
    fun withoutRevenueCatARestoreIsAnError() = runTest {
        assertEquals(PurchaseResult.Error(PurchaseFailure.PURCHASES_UNAVAILABLE), manager.restore())
    }
}
