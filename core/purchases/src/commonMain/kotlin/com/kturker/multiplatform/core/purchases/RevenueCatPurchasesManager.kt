package com.kturker.multiplatform.core.purchases

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.domain.BuildEnvironment
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.DiscountPaymentMode
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.models.freePhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

private val log = Logger.withTag("Purchases")

// RevenueCat is configured the first time Koin creates this class (MainScreen's FirstLaunchPaywall asks for it
// at launch). A release build with a Test Store key skips it on both platforms: RevenueCat's Android SDK would
// close the app, and iOS follows suit (PurchasesKeys).
@Single(binds = [PurchasesManager::class])
internal class RevenueCatPurchasesManager(@Provided environment: BuildEnvironment) : PurchasesManager {

    override val isAvailable: Boolean = PurchasesKeys.canConfigure(
        apiKey = revenueCatApiKey,
        isDebug = environment.isDebug
    )

    private val _isPremium = MutableStateFlow(initialPremium(isAvailable))
    override val isPremium: StateFlow<Boolean?> = _isPremium.asStateFlow()

    // The packages of the last offer(), by id: purchase() needs RevenueCat's Package, the app has only the id
    private var packagesById: Map<String, Package> = emptyMap()

    init {
        if (isAvailable) {
            configure()
        } else {
            warnAboutDisabledPurchases()
        }
    }

    override suspend fun refresh() {
        if (!isAvailable) return
        try {
            update(Purchases.sharedInstance.awaitCustomerInfo())
        } catch (e: PurchasesException) {
            // Offline or not reachable: keep the last known state, the delegate reports the next change.
            log.w { "Could not refresh the customer info: ${e.error.code}" }
        }
    }

    override suspend fun offer(): PaywallOffer? {
        val offering = if (isAvailable) currentOffering() else null
        packagesById = offering?.availablePackages.orEmpty().associateBy { it.identifier }
        return offering?.let { buildOffer(it.availablePackages.map { pkg -> pkg.toFacts() }) }
    }

    override suspend fun purchase(packageId: String): PurchaseResult {
        if (!isAvailable) return PurchaseResult.Error(PurchaseFailure.PURCHASES_UNAVAILABLE)
        // The paywall may have been opened before RevenueCat answered, or the offering changed: ask once more
        if (packageId !in packagesById) offer()
        val pkg = packagesById[packageId]
        return if (pkg == null) PurchaseResult.Error(PurchaseFailure.PACKAGE_UNAVAILABLE) else buy(pkg)
    }

    override suspend fun restore(): PurchaseResult {
        if (!isAvailable) return PurchaseResult.Error(PurchaseFailure.PURCHASES_UNAVAILABLE)
        return try {
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            update(customerInfo)
            restoreResult(premium = hasPremium(activeEntitlementIds = customerInfo.entitlements.active.keys))
        } catch (e: PurchasesException) {
            log.w { "Restore failed: ${e.code}" }
            purchaseResultOf(code = e.code, userCancelled = false)
        }
    }

    private suspend fun currentOffering(): Offering? = try {
        Purchases.sharedInstance.awaitOfferings().current.also { offering ->
            if (offering == null) log.w { "No current offering: mark one current in the RevenueCat dashboard" }
        }
    } catch (e: PurchasesException) {
        log.w { "Could not load the offerings: ${e.code}" }
        null
    }

    private suspend fun buy(pkg: Package): PurchaseResult = try {
        update(Purchases.sharedInstance.awaitPurchase(pkg).customerInfo)
        PurchaseResult.Success
    } catch (e: PurchasesException) {
        log.w { "Purchase failed: ${e.code}" }
        val userCancelled = (e as? PurchasesTransactionException)?.userCancelled == true
        purchaseResultOf(code = e.code, userCancelled = userCancelled)
    }

    private fun configure() {
        if (!Purchases.isConfigured) {
            if (PurchasesKeys.isTestStoreKey(revenueCatApiKey)) {
                Purchases.logLevel = LogLevel.WARN
                warnAboutTestKeys()
            }
            Purchases.configure(PurchasesConfiguration(apiKey = revenueCatApiKey))
        }
        // Setting the delegate also delivers the cached CustomerInfo, if any: a returning user is known offline.
        Purchases.sharedInstance.delegate = object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) = update(customerInfo)

            // App Store promoted purchases are not used by the demo.
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit
                ) -> Unit
            ) = Unit
        }
    }

    private fun update(customerInfo: CustomerInfo) {
        _isPremium.value = hasPremium(activeEntitlementIds = customerInfo.entitlements.active.keys)
    }

    private fun warnAboutTestKeys() {
        log.w { "TEST KEYS - replace revenueCatApiKey (PurchasesKeys.android.kt, .ios.kt) before you ship" }
    }

    private fun warnAboutDisabledPurchases() {
        log.w { "Test Store key in a release build - purchases are disabled; add your platform keys" }
    }
}

private fun Package.toFacts(): PackageFacts = PackageFacts(
    id = identifier,
    type = packageType,
    price = storeProduct.price.formatted,
    priceMicros = storeProduct.price.amountMicros,
    currencyCode = storeProduct.price.currencyCode,
    pricePerMonth = storeProduct.pricePerMonth?.formatted,
    freeTrial = storeProduct.freeTrialPeriod()
)

// Android: the free phase of the default subscription option; iOS: an introductory offer that is a free trial
private fun StoreProduct.freeTrialPeriod(): Period? = defaultOption?.freePhase?.billingPeriod
    ?: introductoryDiscount
        ?.takeIf { it.paymentMode == DiscountPaymentMode.FREE_TRIAL }
        ?.let { Period(it.subscriptionPeriod.value * it.numberOfPeriods.toInt(), it.subscriptionPeriod.unit) }
