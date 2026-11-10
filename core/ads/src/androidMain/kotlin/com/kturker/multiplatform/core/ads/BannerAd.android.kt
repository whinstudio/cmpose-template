package com.kturker.multiplatform.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd as GmaBannerAd

private val log = Logger.withTag("Ads")

// A fixed 320x50 banner, like the iOS one. The AdView loads its ad once and is destroyed when the banner leaves.
@Composable
actual fun BannerAd(modifier: Modifier) {
    if (rememberAdsOffForAutomation()) return
    val activity = requireNotNull(LocalActivity.current) { "Ads need the hosting Activity" }
    val request = remember {
        BannerAdRequest.Builder(AdUnitIds.banner, AdSize.BANNER).build()
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                AdView(activity).apply {
                    loadAd(
                        request,
                        object : AdLoadCallback<GmaBannerAd> {
                            // A failed load leaves the slot empty; the log says why.
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                log.w { "Banner failed to load: ${adError.code} ${adError.message}" }
                            }
                        }
                    )
                }
            },
            onRelease = { it.destroy() }
        )
    }
}
