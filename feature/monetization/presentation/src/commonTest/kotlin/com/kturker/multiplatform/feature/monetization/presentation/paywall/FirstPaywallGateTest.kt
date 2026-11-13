package com.kturker.multiplatform.feature.monetization.presentation.paywall

import com.kturker.multiplatform.core.navigation.LaunchPrompt
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.anyPendingNow
import com.kturker.multiplatform.core.purchases.FirstPaywallStore
import com.kturker.multiplatform.feature.monetization.contract.PaywallDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirstPaywallGateTest {

    private val navigationManager = RecordingNavigationManager()
    private val store = InMemoryFirstPaywallStore()

    private fun gate(purchasesManager: FakePurchasesManager) =
        FirstPaywallGate(purchasesManager = purchasesManager, store = store, navigationManager = navigationManager)

    @Test
    fun opensOnceWhenDue() {
        val gate = gate(FakePurchasesManager(premium = false))

        assertTrue(gate.openIfDue(ready = true, promptPending = false))
        assertFalse(gate.openIfDue(ready = true, promptPending = false))

        assertEquals(listOf<NavigationCommand>(PaywallDestination), navigationManager.commands)
        assertTrue(store.isShown())
    }

    @Test
    fun waitsUntilTheAppIsReady() {
        assertFalse(gate(FakePurchasesManager(premium = false)).openIfDue(ready = false, promptPending = false))

        assertEquals(emptyList<NavigationCommand>(), navigationManager.commands)
        assertFalse(store.isShown())
    }

    // The analytics consent dialog is up (core/insights): the paywall waits for its answer or dismissal
    @Test
    fun waitsWhileALaunchPromptIsPending() {
        val gate = gate(FakePurchasesManager(premium = false))

        assertFalse(gate.openIfDue(ready = true, promptPending = true))
        assertEquals(emptyList<NavigationCommand>(), navigationManager.commands)
        assertFalse(store.isShown())

        // Answered or dismissed: it opens, once
        assertTrue(gate.openIfDue(ready = true, promptPending = false))
        assertFalse(gate.openIfDue(ready = true, promptPending = false))
        assertEquals(listOf<NavigationCommand>(PaywallDestination), navigationManager.commands)
    }

    // An app without core/insights has no launch prompt: the paywall opens as before
    @Test
    fun withoutLaunchPromptsItOpensAsBefore() {
        val gate = gate(FakePurchasesManager(premium = false))
        val prompts = emptyList<LaunchPrompt>()

        assertTrue(gate.openIfDue(ready = true, promptPending = prompts.anyPendingNow()))
    }

    @Test
    fun waitsWhilePremiumIsUnknown() {
        val purchases = FakePurchasesManager(premium = null)
        val gate = gate(purchases)

        assertFalse(gate.openIfDue(ready = true, promptPending = false))
        assertFalse(store.isShown())

        purchases.premiumState.value = false
        assertTrue(gate.openIfDue(ready = true, promptPending = false))
    }

    @Test
    fun neverForPremium() {
        assertFalse(gate(FakePurchasesManager(premium = true)).openIfDue(ready = true, promptPending = false))
        assertFalse(store.isShown())
    }

    @Test
    fun neverWithoutRevenueCat() {
        val gate = gate(FakePurchasesManager(isAvailable = false, premium = false))

        assertFalse(gate.openIfDue(ready = true, promptPending = false))
        assertEquals(emptyList<NavigationCommand>(), navigationManager.commands)
    }

    @Test
    fun notAfterAnEarlierLaunchShowedIt() {
        store.markShown()

        assertFalse(gate(FakePurchasesManager(premium = false)).openIfDue(ready = true, promptPending = false))
    }

    @Test
    fun dueOnlyWhenEverythingAgrees() {
        assertTrue(due())
        assertFalse(due(ready = false))
        assertFalse(due(promptPending = true))
        assertFalse(due(isAvailable = false))
        assertFalse(due(alreadyShown = true))
        assertFalse(due(isPremium = null))
        assertFalse(due(isPremium = true))
    }

    // Every condition due unless a test says otherwise
    private fun due(
        ready: Boolean = true,
        promptPending: Boolean = false,
        isAvailable: Boolean = true,
        alreadyShown: Boolean = false,
        isPremium: Boolean? = false
    ) = shouldOpenFirstPaywall(
        ready = ready,
        promptPending = promptPending,
        isAvailable = isAvailable,
        alreadyShown = alreadyShown,
        isPremium = isPremium
    )
}

private class InMemoryFirstPaywallStore : FirstPaywallStore {
    private var shown = false

    override fun isShown(): Boolean = shown

    override fun markShown() {
        shown = true
    }
}
