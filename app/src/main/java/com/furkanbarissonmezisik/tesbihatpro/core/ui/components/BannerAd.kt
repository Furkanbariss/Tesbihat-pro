package com.furkanbarissonmezisik.tesbihatpro.core.ui.components

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.furkanbarissonmezisik.tesbihatpro.BuildConfig
import com.furkanbarissonmezisik.tesbihatpro.core.ads.AdsController
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAd"

/**
 * Yalnız Geçmiş ve Ayarlar'da kullanılır. İbadet sırasında açılabilecek ekranlara konmaz; bu kural
 * Tesbih ve Zikirler'de reklam kodunun hiç çalışmamasına da dayanır.
 */
@Composable
fun BannerAd(adsController: AdsController, modifier: Modifier = Modifier) {
    val view = LocalView.current
    LaunchedEffect(adsController) {
        adsController.prepare(view.context.findActivity())
    }
    val canShowAds by adsController.canShowAds.collectAsState()
    if (canShowAds) {
        AnchoredBanner(modifier)
    }
}

@Composable
private fun AnchoredBanner(modifier: Modifier) {
    var failed by remember { mutableStateOf(false) }
    if (failed) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        AndroidView(
            factory = { viewContext ->
                AdView(viewContext).apply {
                    adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                    setAdSize(adSize)
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(TAG, "Banner yüklenemedi: ${error.code} ${error.message}")
                            failed = true
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            onRelease = { adView -> adView.destroy() },
        )
    }
}
