package com.kturker.multiplatform.feature.monetization.presentation.paywall

import com.kturker.multiplatform.core.purchases.PackagePeriod
import com.kturker.multiplatform.core.purchases.PaywallOffer
import com.kturker.multiplatform.core.purchases.PaywallPackage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val WEEKLY_PLAN = PaywallPackage("weekly", PackagePeriod.WEEKLY, "$2.99", "$12.99", null, null)
private val MONTHLY_PLAN = PaywallPackage("monthly", PackagePeriod.MONTHLY, "$4.99", null, null, null)
private val ANNUAL_PLAN = PaywallPackage("annual", PackagePeriod.ANNUAL, "$29.99", "$2.49", 50, null)
private val LIFETIME_PLAN = PaywallPackage("lifetime", PackagePeriod.LIFETIME, "$49.99", null, null, null)
private val OTHER_PLAN = PaywallPackage("quarterly", PackagePeriod.OTHER, "$9.99", null, null, null)
private val EVERY_PLAN = PaywallOffer(listOf(LIFETIME_PLAN, ANNUAL_PLAN, MONTHLY_PLAN, WEEKLY_PLAN, OTHER_PLAN))
private val EVERY_PERIOD = PackagePeriod.entries.toSet()
private val ONE_TIME = setOf(PackagePeriod.LIFETIME)
private val SUBSCRIPTION = setOf(PackagePeriod.WEEKLY, PackagePeriod.MONTHLY, PackagePeriod.ANNUAL)

class PaywallPlansTest {
    @Test
    fun everyPackageIsOfferedWhenEveryPeriodIsAllowed() {
        assertEquals(EVERY_PLAN, offeredPackages(EVERY_PLAN, EVERY_PERIOD))
    }

    @Test
    fun aOneTimeAppOffersOnlyLifetimeWithoutRenewalTerms() {
        val offer = assertNotNull(offeredPackages(EVERY_PLAN, ONE_TIME))
        assertEquals(listOf(LIFETIME_PLAN), offer.packages)
        assertEquals(LIFETIME_PLAN.id, defaultSelection(offer.packages))
        assertFalse(PaywallUiState(isAvailable = true, offer = offer).showsRenewalTerms)
    }

    @Test
    fun aSubscriptionAppOffersNoLifetimeAndSelectsAnnual() {
        val offer = assertNotNull(offeredPackages(EVERY_PLAN, SUBSCRIPTION))
        assertEquals(listOf(ANNUAL_PLAN, MONTHLY_PLAN, WEEKLY_PLAN), offer.packages)
        assertEquals(ANNUAL_PLAN.id, defaultSelection(offer.packages))
    }

    @Test
    fun otherPeriodsShowOnlyWhenEveryPackageIsOffered() {
        assertTrue(OTHER_PLAN in offeredPackages(EVERY_PLAN, EVERY_PERIOD)!!.packages)
        assertFalse(OTHER_PLAN in offeredPackages(EVERY_PLAN, SUBSCRIPTION)!!.packages)
        assertFalse(OTHER_PLAN in offeredPackages(EVERY_PLAN, ONE_TIME)!!.packages)
    }

    @Test
    fun anOfferingWithoutTheAllowedPeriodsIsUnavailable() {
        assertNull(offeredPackages(PaywallOffer(listOf(ANNUAL_PLAN, MONTHLY_PLAN)), ONE_TIME))
        assertNull(offeredPackages(PaywallOffer(listOf(LIFETIME_PLAN)), SUBSCRIPTION))
        assertNull(offeredPackages(null, EVERY_PERIOD))
    }
}
