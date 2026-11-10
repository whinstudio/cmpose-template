package com.kturker.multiplatform.core.multilang.resource.res

import com.kturker.multiplatform.core.multilang.resource.StringResourcesUiModel

internal val resourceES = StringResourcesUiModel(
    settingsTitle = "Configuración",
    themeLight = "Claro",
    themeSystem = "Sistema",
    themeDark = "Oscuro",
    languageSystem = "Sistema",
    offlineBanner = "Sin conexión",
    aboutTitle = "Acerca de",
    settingsVersion = "Versión",
    settingsPrivacyPolicy = "Política de privacidad",
    settingsTerms = "Términos de uso",
    settingsLicenses = "Licencias de código abierto",
    close = "Cerrar",
    // cmpose:purchases
    settingsPremium = "Premium",
    retry = "Reintentar",
    paywallNotNow = "Ahora no",
    paywallHeadline = "Desbloquea Premium",
    paywallBenefitRemoveAds = "Elimina los anuncios",
    paywallBenefitOne = "Función premium uno",
    paywallBenefitTwo = "Función premium dos",
    paywallContinue = "Continuar",
    paywallRestore = "Restaurar compras",
    paywallPurchasesDisabled = "Las compras están desactivadas en las compilaciones de release que usan una clave de " +
        "RevenueCat Test Store. Añade tus claves de App Store / Play Store en PurchasesKeys.android.kt y " +
        "PurchasesKeys.ios.kt.",
    paywallRenewalTerms = "Las suscripciones se renuevan automáticamente salvo que se cancelen al menos 24 horas " +
        "antes del final del periodo actual. Gestiónalas o cancélalas en los ajustes de tu cuenta de App Store o " +
        "Google Play.",
    paywallMissingLegalLinks = "Define PRIVACY_POLICY_URL y TERMS_URL en LegalUrls.kt antes de publicar: las tiendas " +
        "exigen ambos enlaces en esta pantalla.",
    paywallPeriodWeekly = "Semanal",
    paywallPeriodMonthly = "Mensual",
    paywallPeriodAnnual = "Anual",
    paywallPeriodLifetime = "De por vida",
    paywallPeriodOther = "Plan",
    paywallPriceWeekly = "{price} / semana",
    paywallPriceMonthly = "{price} / mes",
    paywallPriceAnnual = "{price} / año",
    paywallPriceLifetime = "{price} pago único",
    paywallPricePerMonth = "{price}/mes",
    paywallSave = "Ahorra un {percent} %",
    paywallFreeTrial = "{trial} gratis",
    paywallPlansUnavailable = "No se han podido cargar los planes. Comprueba tu conexión e inténtalo de nuevo.",
    purchaseErrorUnavailable = "Las compras no están disponibles en esta compilación",
    purchaseErrorPackageUnavailable = "Este plan no está disponible ahora. Inténtalo más tarde.",
    purchaseErrorNothingToRestore = "No hay compras que restaurar",
    purchaseErrorNetwork = "Sin conexión. Comprueba tu internet e inténtalo de nuevo.",
    purchaseErrorPending = "Pago pendiente: Premium se activará cuando la tienda lo confirme.",
    purchaseErrorStore = "La tienda no responde. Inténtalo más tarde.",
    purchaseErrorNotAllowed = "Las compras no están permitidas en este dispositivo.",
    purchaseErrorFailed = "La compra ha fallado. Inténtalo de nuevo.",
    // /cmpose:purchases
    // cmpose:ads
    settingsAdExamples = "Ejemplos de anuncios",
    back = "Atrás",
    adsPremiumOff = "Premium: anuncios desactivados",
    adsShowInterstitial = "Mostrar intersticial",
    adsWatchRewarded = "Ver anuncio con recompensa",
    adsPrivacyOptions = "Opciones de privacidad",
    adsCoins = "Monedas: {coins}",
    adsTestNote = "Anuncios de prueba (unidades de prueba de Google). Las compilaciones de depuración fuerzan el " +
        "formulario de consentimiento del EEE.",
    adsConsentFailed = "Anuncios no disponibles: no se pudo obtener el consentimiento",
    adsInterstitialPaced = "Intersticial limitado: no en el primer minuto, como máximo uno por sesión",
    adsInterstitialSkipped = "Premium: intersticial omitido",
    adsFailed = "Error del anuncio: {reason}",
    // /cmpose:ads
    updateAvailableTitle = "Actualización disponible",
    updateAvailableMessage = "Hay una nueva versión de la app.",
    updateNow = "Actualizar",
    updateLater = "Más tarde",
    updateRequiredTitle = "Actualización necesaria",
    updateRequiredMessage = "Esta versión ya no es compatible. Actualiza la app para continuar.",
    updateDownloaded = "Actualización descargada",
    updateRestart = "Reiniciar"
)
