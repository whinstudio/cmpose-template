package com.kturker.multiplatform.core.multilang.resource.res

import com.kturker.multiplatform.core.multilang.resource.StringResourcesUiModel

internal val resourceFR = StringResourcesUiModel(
    settingsTitle = "Paramètres",
    themeLight = "Clair",
    themeSystem = "Système",
    themeDark = "Sombre",
    languageSystem = "Système",
    offlineBanner = "Pas de connexion",
    aboutTitle = "À propos",
    settingsVersion = "Version",
    settingsPrivacyPolicy = "Politique de confidentialité",
    settingsTerms = "Conditions d’utilisation",
    settingsLicenses = "Licences open source",
    close = "Fermer",
    // cmpose:purchases
    settingsPremium = "Premium",
    retry = "Réessayer",
    paywallNotNow = "Pas maintenant",
    paywallHeadline = "Débloquez Premium",
    paywallBenefitRemoveAds = "Supprimez les publicités",
    paywallBenefitOne = "Fonctionnalité premium un",
    paywallBenefitTwo = "Fonctionnalité premium deux",
    paywallContinue = "Continuer",
    paywallRestore = "Restaurer les achats",
    paywallPurchasesDisabled = "Les achats sont désactivés dans les builds de release qui utilisent une clé " +
        "RevenueCat Test Store. Ajoutez vos clés App Store / Play Store dans PurchasesKeys.android.kt et " +
        "PurchasesKeys.ios.kt.",
    paywallRenewalTerms = "Les abonnements se renouvellent automatiquement, sauf annulation au moins 24 heures avant " +
        "la fin de la période en cours. Gérez-les ou annulez-les dans les réglages de votre compte App Store ou " +
        "Google Play.",
    paywallMissingLegalLinks = "Définissez PRIVACY_POLICY_URL et TERMS_URL dans LegalUrls.kt avant de publier : les " +
        "stores exigent les deux liens sur cet écran.",
    paywallPeriodWeekly = "Hebdomadaire",
    paywallPeriodMonthly = "Mensuel",
    paywallPeriodAnnual = "Annuel",
    paywallPeriodLifetime = "À vie",
    paywallPeriodOther = "Forfait",
    paywallPriceWeekly = "{price} / semaine",
    paywallPriceMonthly = "{price} / mois",
    paywallPriceAnnual = "{price} / an",
    paywallPriceLifetime = "{price} en une fois",
    paywallPricePerMonth = "{price}/mois",
    paywallSave = "Économisez {percent} %",
    paywallFreeTrial = "{trial} gratuits",
    paywallPlansUnavailable = "Impossible de charger les forfaits. Vérifiez votre connexion et réessayez.",
    purchaseErrorUnavailable = "Les achats ne sont pas disponibles dans cette version",
    purchaseErrorPackageUnavailable = "Ce forfait n'est pas disponible pour le moment. Réessayez plus tard.",
    purchaseErrorNothingToRestore = "Aucun achat à restaurer",
    purchaseErrorNetwork = "Pas de connexion. Vérifiez votre accès à Internet et réessayez.",
    purchaseErrorPending = "Paiement en attente : Premium sera débloqué une fois confirmé par le store.",
    purchaseErrorStore = "Le store ne répond pas. Réessayez plus tard.",
    purchaseErrorNotAllowed = "Les achats ne sont pas autorisés sur cet appareil.",
    purchaseErrorFailed = "L'achat a échoué. Réessayez.",
    // /cmpose:purchases
    // cmpose:ads
    settingsAdExamples = "Exemples d’annonces",
    back = "Retour",
    adsPremiumOff = "Premium : publicités désactivées",
    adsShowInterstitial = "Afficher un interstitiel",
    adsWatchRewarded = "Regarder une publicité récompensée",
    adsPrivacyOptions = "Options de confidentialité",
    adsCoins = "Pièces : {coins}",
    adsTestNote = "Publicités de test (blocs de test Google). Les builds de débogage imposent le formulaire de " +
        "consentement EEE.",
    adsConsentFailed = "Publicités indisponibles : le consentement n'a pas pu être recueilli",
    adsInterstitialPaced = "Interstitiel limité : pas pendant la première minute, un au maximum par session",
    adsInterstitialSkipped = "Premium : interstitiel ignoré",
    adsFailed = "Échec de la publicité : {reason}",
    // /cmpose:ads
    updateAvailableTitle = "Mise à jour disponible",
    updateAvailableMessage = "Une nouvelle version de l’app est disponible.",
    updateNow = "Mettre à jour",
    updateLater = "Plus tard",
    updateRequiredTitle = "Mise à jour requise",
    updateRequiredMessage = "Cette version n’est plus prise en charge. Mettez l’app à jour pour continuer.",
    updateDownloaded = "Mise à jour téléchargée",
    updateRestart = "Redémarrer"
)
