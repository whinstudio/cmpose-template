package com.kturker.multiplatform.core.ads

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView

// The standard banner is 320x50 points; the caller places (centers) it.
private const val BANNER_WIDTH = 320
private const val BANNER_HEIGHT = 50

@Composable
actual fun BannerAd(modifier: Modifier) {
    if (rememberAdsOffForAutomation()) return
    UIKitView(
        factory = { IosAds.requireBridge().makeBanner(AdUnitIds.banner) },
        modifier = modifier.size(width = BANNER_WIDTH.dp, height = BANNER_HEIGHT.dp),
        // Interop views are hidden from accessibility otherwise, so UI tests could not find the banner id.
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true)
    )
}
