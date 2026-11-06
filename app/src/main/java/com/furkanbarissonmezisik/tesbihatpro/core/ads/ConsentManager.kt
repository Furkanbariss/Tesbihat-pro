package com.furkanbarissonmezisik.tesbihatpro.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.furkanbarissonmezisik.tesbihatpro.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

interface ConsentManager {
    val canRequestAds: Boolean
    val privacyOptionsRequired: Boolean

    /** Onay bilgisini günceller, gerekiyorsa formu gösterir; hata olsa da [onFinished] çağrılır. */
    fun gatherConsent(activity: Activity, onFinished: () -> Unit)

    fun showPrivacyOptions(activity: Activity, onFinished: () -> Unit)
}

class UmpConsentManager(context: Context) : ConsentManager {
    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    override val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    override val privacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun gatherConsent(activity: Activity, onFinished: () -> Unit) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            requestParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Log.e(TAG, "Onay formu gösterilemedi: ${it.message}") }
                    onFinished()
                }
            },
            { requestError ->
                Log.e(TAG, "Onay bilgisi alınamadı: ${requestError.message}")
                onFinished()
            },
        )
    }

    override fun showPrivacyOptions(activity: Activity, onFinished: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Log.e(TAG, "Gizlilik seçenekleri açılamadı: ${it.message}") }
            onFinished()
        }
    }

    /** AB simülasyonu yalnız debug'da ve local.properties'te cihaz kimliği varsa devreye girer. */
    private fun requestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.UMP_TEST_DEVICE_HASH.isNotEmpty()) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(BuildConfig.UMP_TEST_DEVICE_HASH)
                    .build(),
            )
        }
        return builder.build()
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}
