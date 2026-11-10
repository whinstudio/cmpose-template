package com.kturker.multiplatform.feature.monetization.presentation.paywall

import androidx.compose.runtime.staticCompositionLocalOf
import com.kturker.multiplatform.core.purchases.PackagePeriod
import com.kturker.multiplatform.core.purchases.PurchaseFailure

// The paywall's texts; the defaults are English. The showcase passes its translations.
// {price}, {percent} and {trial} are filled in by the screen; prices keep the store's own format.
data class PaywallTexts(
    val title: String = "Premium",
    val notNow: String = "Not now",
    val retry: String = "Try again",
    val headline: String = "Unlock Premium",
    // Placeholder benefits: list what premium unlocks in your app
    val benefitRemoveAds: String = "Remove ads",
    val benefitOne: String = "Premium feature one",
    val benefitTwo: String = "Premium feature two",
    val continueLabel: String = "Continue",
    val restore: String = "Restore purchases",
    val purchasesDisabled: String =
        "Purchases are disabled in release builds that use a RevenueCat Test Store key. " +
            "Add your App Store / Play Store keys in PurchasesKeys.android.kt and PurchasesKeys.ios.kt.",
    // Shown under the plans whenever one of them renews: the App Store and Google Play want these terms here
    val renewalTerms: String =
        "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period. " +
            "Manage or cancel them in your App Store or Google Play account settings.",
    // Debug builds only, until LegalUrls.kt has both links
    val missingLegalLinks: String =
        "Set PRIVACY_POLICY_URL and TERMS_URL in LegalUrls.kt before you ship: " +
            "the stores want both links on this screen.",
    val privacyPolicy: String = "Privacy Policy",
    val terms: String = "Terms of Use",
    val periodWeekly: String = "Weekly",
    val periodMonthly: String = "Monthly",
    val periodAnnual: String = "Annual",
    val periodLifetime: String = "Lifetime",
    val periodOther: String = "Plan",
    val priceWeekly: String = "{price} / week",
    val priceMonthly: String = "{price} / month",
    val priceAnnual: String = "{price} / year",
    val priceLifetime: String = "{price} once",
    val pricePerMonth: String = "{price}/month",
    val save: String = "Save {percent}%",
    val freeTrial: String = "{trial} free",
    val plansUnavailable: String = "Plans could not be loaded. Check your connection and try again.",
    val failureUnavailable: String = "Purchases are not available in this build",
    val failurePackageUnavailable: String = "This plan is not available right now. Try again later.",
    val failureNothingToRestore: String = "No purchases to restore",
    val failureNetwork: String = "No connection. Check your internet and try again.",
    val failurePending: String = "Payment pending: premium unlocks once the store confirms it.",
    val failureStore: String = "The store is not responding. Try again later.",
    val failureNotAllowed: String = "Purchases are not allowed on this device.",
    val failureFailed: String = "Purchase failed. Try again."
)

val LocalPaywallTexts = staticCompositionLocalOf { PaywallTexts() }

internal fun PaywallTexts.message(error: PaywallError): String = when (error) {
    PaywallError.PlansUnavailable -> plansUnavailable

    is PaywallError.Purchase -> when (error.failure) {
        PurchaseFailure.PURCHASES_UNAVAILABLE -> failureUnavailable
        PurchaseFailure.PACKAGE_UNAVAILABLE -> failurePackageUnavailable
        PurchaseFailure.NOTHING_TO_RESTORE -> failureNothingToRestore
        PurchaseFailure.NETWORK -> failureNetwork
        PurchaseFailure.PAYMENT_PENDING -> failurePending
        PurchaseFailure.STORE_PROBLEM -> failureStore
        PurchaseFailure.NOT_ALLOWED -> failureNotAllowed
        PurchaseFailure.FAILED -> failureFailed
    }
}

internal fun PaywallTexts.periodLabel(period: PackagePeriod): String = when (period) {
    PackagePeriod.WEEKLY -> periodWeekly
    PackagePeriod.MONTHLY -> periodMonthly
    PackagePeriod.ANNUAL -> periodAnnual
    PackagePeriod.LIFETIME -> periodLifetime
    PackagePeriod.OTHER -> periodOther
}

// OTHER has no period to name: the price stands alone
internal fun PaywallTexts.priceLabel(
    price: String,
    period: PackagePeriod
): String = when (period) {
    PackagePeriod.WEEKLY -> priceWeekly.replace("{price}", price)
    PackagePeriod.MONTHLY -> priceMonthly.replace("{price}", price)
    PackagePeriod.ANNUAL -> priceAnnual.replace("{price}", price)
    PackagePeriod.LIFETIME -> priceLifetime.replace("{price}", price)
    PackagePeriod.OTHER -> price
}

internal fun PaywallTexts.perMonthLabel(price: String): String =
    pricePerMonth.replace("{price}", price)

internal fun PaywallTexts.saveLabel(percent: Int): String =
    save.replace("{percent}", percent.toString())

internal fun PaywallTexts.freeTrialLabel(trial: String): String =
    freeTrial.replace("{trial}", trial)
