package com.kturker.multiplatform.core.purchases

import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals

class PurchaseMessagesTest {

    @Test
    fun aUserWhoBacksOutHasCancelled() {
        assertEquals(PurchaseResult.Cancelled, purchaseResultOf(code = PurchasesErrorCode.UnknownError, userCancelled = true))
        assertEquals(PurchaseResult.Cancelled, purchaseResultOf(code = PurchasesErrorCode.PurchaseCancelledError, userCancelled = false))
    }

    @Test
    fun errorsBecomeFailureReasons() {
        val cases = mapOf(
            PurchasesErrorCode.NetworkError to PurchaseFailure.NETWORK,
            PurchasesErrorCode.OfflineConnectionError to PurchaseFailure.NETWORK,
            PurchasesErrorCode.PaymentPendingError to PurchaseFailure.PAYMENT_PENDING,
            PurchasesErrorCode.StoreProblemError to PurchaseFailure.STORE_PROBLEM,
            PurchasesErrorCode.ProductRequestTimedOut to PurchaseFailure.STORE_PROBLEM,
            PurchasesErrorCode.PurchaseNotAllowedError to PurchaseFailure.NOT_ALLOWED,
            PurchasesErrorCode.ProductNotAvailableForPurchaseError to PurchaseFailure.PACKAGE_UNAVAILABLE,
            PurchasesErrorCode.TestStoreSimulatedPurchaseError to PurchaseFailure.FAILED,
            PurchasesErrorCode.UnknownError to PurchaseFailure.FAILED
        )
        for ((code, failure) in cases) {
            assertEquals(PurchaseResult.Error(failure), purchaseResultOf(code = code, userCancelled = false), code.name)
        }
    }

    @Test
    fun aRestoreSucceedsOnlyWithPremium() {
        assertEquals(PurchaseResult.Success, restoreResult(premium = true))
        assertEquals(PurchaseResult.Error(PurchaseFailure.NOTHING_TO_RESTORE), restoreResult(premium = false))
    }
}
