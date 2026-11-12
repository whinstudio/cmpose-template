package com.kturker.multiplatform.feature.monetization.presentation.paywall

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.domain.BuildEnvironment
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.core.purchases.PurchaseResult
import com.kturker.multiplatform.core.purchases.PurchasesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

// The paywall's behaviour on PurchasesManager alone (no RevenueCat type): load the offer, buy or restore, close.
// Your app redraws PaywallScreen in its own design and keeps this ViewModel.
@KoinViewModel
internal class PaywallViewModel(
    @Provided environment: BuildEnvironment,
    @Provided private val navigationManager: NavigationManager,
    @Provided private val purchasesManager: PurchasesManager,
    @Provided appInfo: AppInfo
) : CoreViewModel(),
    PaywallActions {

    private val _uiState = MutableStateFlow(
        PaywallUiState(
            isAvailable = purchasesManager.isAvailable,
            legalLinks = appInfo.legalLinks,
            missingLegalLinks = environment.isDebug && appInfo.legalLinks.size < 2
        )
    )
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    // A successful purchase closes the paywall; a Not now right after must not close the screen below it
    private var isClosed = false

    init {
        loadOffer()
    }

    override fun onRetry() = loadOffer()

    override fun onPackageSelected(packageId: String) {
        if (_uiState.value.inProgress) return
        _uiState.update { it.copy(selectedPackageId = packageId, error = null) }
    }

    override fun onPurchaseClick() {
        val packageId = _uiState.value.selectedPackageId ?: return
        runPurchase { purchasesManager.purchase(packageId) }
    }

    override fun onRestoreClick() = runPurchase { purchasesManager.restore() }

    override fun onDismiss() = close()

    private fun loadOffer() {
        if (!purchasesManager.isAvailable) return
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val offer = offeredPackages(purchasesManager.offer(), PAYWALL_PERIODS)
            _uiState.update { state ->
                if (offer == null || offer.packages.isEmpty()) {
                    state.copy(
                        loading = false,
                        offer = null,
                        selectedPackageId = null,
                        error = PaywallError.PlansUnavailable
                    )
                } else {
                    state.copy(
                        loading = false,
                        offer = offer,
                        selectedPackageId = defaultSelection(offer.packages),
                        error = null
                    )
                }
            }
        }
    }

    private fun runPurchase(call: suspend () -> PurchaseResult) {
        if (_uiState.value.inProgress || isClosed) return
        _uiState.update { it.copy(inProgress = true, error = null) }
        viewModelScope.launch {
            val result = call()
            val error = (result as? PurchaseResult.Error)?.let { PaywallError.Purchase(it.failure) }
            _uiState.update { it.copy(inProgress = false, error = error) }
            if (result == PurchaseResult.Success) close()
        }
    }

    private fun close() {
        if (isClosed) return
        isClosed = true
        navigationManager.navigate(NavigationCommand.NavigateUp)
    }
}
