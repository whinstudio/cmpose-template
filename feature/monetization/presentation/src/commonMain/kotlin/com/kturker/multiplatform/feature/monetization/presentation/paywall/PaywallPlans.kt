package com.kturker.multiplatform.feature.monetization.presentation.paywall

import com.kturker.multiplatform.core.purchases.PackagePeriod
import com.kturker.multiplatform.core.purchases.PaywallOffer

// The kinds of plan the paywall offers: only the current offering's packages whose period is in this set are
// shown. Every package by default; setOf(PackagePeriod.LIFETIME) for a one-time purchase;
// setOf(PackagePeriod.WEEKLY, PackagePeriod.MONTHLY, PackagePeriod.ANNUAL) for a subscription.
// Keep the declaration on one line: generators rewrite it.
internal val PAYWALL_PERIODS = PackagePeriod.entries.toSet()

// The offer cut to the periods the app sells; null when there is no offer or nothing is left, so the paywall shows
// "plans unavailable" rather than plans the app does not sell
internal fun offeredPackages(
    offer: PaywallOffer?,
    periods: Set<PackagePeriod>
): PaywallOffer? =
    offer?.packages?.filter { it.period in periods }?.takeIf { it.isNotEmpty() }?.let(::PaywallOffer)
