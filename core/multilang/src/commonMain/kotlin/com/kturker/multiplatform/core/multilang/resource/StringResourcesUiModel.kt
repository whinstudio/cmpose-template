package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.domain.DataError

/**
 * Every UI string of the app, one instance per language (see `resource/res`). The properties have
 * no default values on purpose: a language that misses a string does not compile.
 */
data class StringResourcesUiModel(
    val settingsTitle: String,
    val githubSearchHint: String,
    val themeLight: String,
    val themeSystem: String,
    val themeDark: String,
    val languageSystem: String,
    val statFollowers: String,
    val statFollowing: String,
    val statRepos: String,
    val statGists: String,
    val errorNoConnection: String,
    val errorRateLimit: String,
    val errorServer: String,
    val errorUnexpected: String,
    val settingsPremium: String,
    val settingsAdExamples: String,
    val settingsRateApp: String,
    val settingsVersion: String,
    val settingsPrivacyPolicy: String,
    val settingsTerms: String,
    val settingsLicenses: String,
    val offlineBanner: String,
    val updateAvailableTitle: String,
    val updateAvailableMessage: String,
    val updateNow: String,
    val updateLater: String,
    val updateRequiredTitle: String,
    val updateRequiredMessage: String,
    val updateDownloaded: String,
    val updateRestart: String,
    val favoritesTitle: String,
    val addFavorite: String,
    val removeFavorite: String,
    val retry: String,
    val back: String,
    val paywallNotNow: String,
    val paywallHeadline: String,
    val paywallBenefitRemoveAds: String,
    val paywallBenefitOne: String,
    val paywallBenefitTwo: String,
    val paywallContinue: String,
    val paywallRestore: String,
    val paywallPurchasesDisabled: String,
    val paywallRenewalTerms: String,
    val paywallMissingLegalLinks: String,
    val paywallPeriodWeekly: String,
    val paywallPeriodMonthly: String,
    val paywallPeriodAnnual: String,
    val paywallPeriodLifetime: String,
    val paywallPeriodOther: String,
    val paywallPriceWeekly: String,
    val paywallPriceMonthly: String,
    val paywallPriceAnnual: String,
    val paywallPriceLifetime: String,
    val paywallPricePerMonth: String,
    val paywallSave: String,
    val paywallFreeTrial: String,
    val paywallPlansUnavailable: String,
    val purchaseErrorUnavailable: String,
    val purchaseErrorPackageUnavailable: String,
    val purchaseErrorNothingToRestore: String,
    val purchaseErrorNetwork: String,
    val purchaseErrorPending: String,
    val purchaseErrorStore: String,
    val purchaseErrorNotAllowed: String,
    val purchaseErrorFailed: String,
    val adsPremiumOff: String,
    val adsShowInterstitial: String,
    val adsWatchRewarded: String,
    val adsPrivacyOptions: String,
    val adsCoins: String,
    val adsTestNote: String,
    val adsConsentFailed: String,
    val adsInterstitialPaced: String,
    val adsInterstitialSkipped: String,
    val adsFailed: String,
    val searchPromptTitle: String,
    val searchPromptMessage: String,
    val searchNoResultsTitle: String,
    val searchNoResultsMessage: String,
    val favoritesEmptyTitle: String,
    val favoritesEmptyMessage: String
)

// GitHub answers 403 (primary limit) or 429 (secondary limit) once its request limit is used up
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429

/** The message a screen shows for [error]; Home and Onboarding both take it from here. */
fun StringResourcesUiModel.errorMessage(error: DataError): String = when (error) {
    DataError.Network -> errorNoConnection
    is DataError.Http -> if (error.isRateLimit()) errorRateLimit else errorServer
    DataError.Serialization, is DataError.Unknown -> errorUnexpected
}

private fun DataError.Http.isRateLimit(): Boolean =
    code == HTTP_FORBIDDEN || code == HTTP_TOO_MANY_REQUESTS
