package com.kturker.multiplatform.core.multilang.resource.res

import com.kturker.multiplatform.core.multilang.resource.StringResourcesUiModel

internal val resourceIT = StringResourcesUiModel(
    settingsTitle = "Impostazioni",
    themeLight = "Chiaro",
    themeSystem = "Sistema",
    themeDark = "Scuro",
    languageSystem = "Sistema",
    offlineBanner = "Nessuna connessione",
    aboutTitle = "Informazioni",
    settingsVersion = "Versione",
    settingsPrivacyPolicy = "Informativa sulla privacy",
    settingsTerms = "Termini di utilizzo",
    settingsLicenses = "Licenze open source",
    close = "Chiudi",
    // cmpose:purchases
    settingsPremium = "Premium",
    retry = "Riprova",
    paywallNotNow = "Non ora",
    paywallHeadline = "Sblocca Premium",
    paywallBenefitRemoveAds = "Rimuovi gli annunci",
    paywallBenefitOne = "Funzione premium uno",
    paywallBenefitTwo = "Funzione premium due",
    paywallContinue = "Continua",
    paywallRestore = "Ripristina acquisti",
    paywallPurchasesDisabled = "Gli acquisti sono disattivati nelle build di release che usano una chiave RevenueCat " +
        "Test Store. Aggiungi le chiavi di App Store / Play Store in PurchasesKeys.android.kt e PurchasesKeys.ios.kt.",
    paywallRenewalTerms = "Gli abbonamenti si rinnovano automaticamente se non vengono annullati almeno 24 ore prima " +
        "della fine del periodo in corso. Gestiscili o annullali nelle impostazioni del tuo account App Store o " +
        "Google Play.",
    paywallMissingLegalLinks = "Imposta PRIVACY_POLICY_URL e TERMS_URL in LegalUrls.kt prima della pubblicazione: " +
        "gli store richiedono entrambi i link in questa schermata.",
    paywallPeriodWeekly = "Settimanale",
    paywallPeriodMonthly = "Mensile",
    paywallPeriodAnnual = "Annuale",
    paywallPeriodLifetime = "A vita",
    paywallPeriodOther = "Piano",
    paywallPriceWeekly = "{price} / settimana",
    paywallPriceMonthly = "{price} / mese",
    paywallPriceAnnual = "{price} / anno",
    paywallPriceLifetime = "{price} una tantum",
    paywallPricePerMonth = "{price}/mese",
    paywallSave = "Risparmia il {percent}%",
    paywallFreeTrial = "{trial} gratis",
    paywallPlansUnavailable = "Impossibile caricare i piani. Controlla la connessione e riprova.",
    purchaseErrorUnavailable = "Gli acquisti non sono disponibili in questa build",
    purchaseErrorPackageUnavailable = "Questo piano non è disponibile al momento. Riprova più tardi.",
    purchaseErrorNothingToRestore = "Nessun acquisto da ripristinare",
    purchaseErrorNetwork = "Nessuna connessione. Controlla internet e riprova.",
    purchaseErrorPending = "Pagamento in sospeso: Premium si sblocca quando lo store lo conferma.",
    purchaseErrorStore = "Lo store non risponde. Riprova più tardi.",
    purchaseErrorNotAllowed = "Gli acquisti non sono consentiti su questo dispositivo.",
    purchaseErrorFailed = "Acquisto non riuscito. Riprova.",
    // /cmpose:purchases
    // cmpose:ads
    settingsAdExamples = "Esempi di annunci",
    back = "Indietro",
    adsPremiumOff = "Premium: annunci disattivati",
    adsShowInterstitial = "Mostra interstitial",
    adsWatchRewarded = "Guarda annuncio con premio",
    adsPrivacyOptions = "Opzioni privacy",
    adsCoins = "Monete: {coins}",
    adsTestNote = "Annunci di prova (unità di prova Google). Le build di debug forzano il modulo di consenso SEE.",
    adsConsentFailed = "Annunci non disponibili: impossibile raccogliere il consenso",
    adsInterstitialPaced = "Interstitial limitato: non nel primo minuto, al massimo uno per sessione",
    adsInterstitialSkipped = "Premium: interstitial saltato",
    adsFailed = "Annuncio non riuscito: {reason}",
    // /cmpose:ads
    updateAvailableTitle = "Aggiornamento disponibile",
    updateAvailableMessage = "È disponibile una nuova versione dell’app.",
    updateNow = "Aggiorna",
    updateLater = "Più tardi",
    updateRequiredTitle = "Aggiornamento necessario",
    updateRequiredMessage = "Questa versione non è più supportata. Aggiorna l’app per continuare.",
    updateDownloaded = "Aggiornamento scaricato",
    updateRestart = "Riavvia"
)
