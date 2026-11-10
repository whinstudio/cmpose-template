package com.kturker.multiplatform.core.purchases

sealed interface PurchaseResult {
    // isPremium follows on its own
    data object Success : PurchaseResult

    // The user backed out of the store's sheet: show nothing
    data object Cancelled : PurchaseResult

    // Why it did not go through: the screen picks the text
    data class Error(val failure: PurchaseFailure) : PurchaseResult
}

// The reasons a purchase or restore fails, as a screen tells them apart
enum class PurchaseFailure {
    PURCHASES_UNAVAILABLE,
    PACKAGE_UNAVAILABLE,
    NOTHING_TO_RESTORE,
    NETWORK,
    PAYMENT_PENDING,
    STORE_PROBLEM,
    NOT_ALLOWED,
    FAILED
}
