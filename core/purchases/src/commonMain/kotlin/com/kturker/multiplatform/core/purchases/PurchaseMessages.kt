package com.kturker.multiplatform.core.purchases

import com.revenuecat.purchases.kmp.models.PurchasesErrorCode

// core/purchases carries a reason, the screen picks its text: core/purchases does not depend on
// core/multilang, which a project may not have.

// A purchase or restore that did not go through, as the paywall shows it
internal fun purchaseResultOf(
    code: PurchasesErrorCode,
    userCancelled: Boolean
): PurchaseResult = if (userCancelled || code == PurchasesErrorCode.PurchaseCancelledError) {
    PurchaseResult.Cancelled
} else {
    PurchaseResult.Error(failureOf(code))
}

private fun failureOf(code: PurchasesErrorCode): PurchaseFailure = when (code) {
    PurchasesErrorCode.NetworkError, PurchasesErrorCode.OfflineConnectionError -> PurchaseFailure.NETWORK
    PurchasesErrorCode.PaymentPendingError -> PurchaseFailure.PAYMENT_PENDING
    PurchasesErrorCode.StoreProblemError, PurchasesErrorCode.ProductRequestTimedOut -> PurchaseFailure.STORE_PROBLEM
    PurchasesErrorCode.PurchaseNotAllowedError -> PurchaseFailure.NOT_ALLOWED
    PurchasesErrorCode.ProductNotAvailableForPurchaseError -> PurchaseFailure.PACKAGE_UNAVAILABLE
    else -> PurchaseFailure.FAILED
}

// A restore succeeds only when it brings premium back
internal fun restoreResult(premium: Boolean): PurchaseResult =
    if (premium) PurchaseResult.Success else PurchaseResult.Error(PurchaseFailure.NOTHING_TO_RESTORE)
