package com.furkanbarissonmezisik.tesbihatpro

import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ThemeMode
import com.furkanbarissonmezisik.tesbihatpro.core.ui.theme.TesbihatProTheme
import com.furkanbarissonmezisik.tesbihatpro.navigation.AppNavHost
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val container get() = (application as TesbihApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Küçük bir dosya okunuyor; beklenmezse ilk kare yanlış temayla çizilir.
        runBlocking { container.settingsRepository.awaitLoaded() }
        setContent {
            val settings by container.settingsRepository.settings.collectAsState()
            val darkTheme = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(settings.theme) { tellSystemTheme(settings.theme) }
            // Sistem çubuklarının simgeleri sistemin değil, seçilen temanın rengine uymalı.
            DisposableEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }
            TesbihatProTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }

    /**
     * Açılış ekranını sistem, uygulama kodu çalışmadan çizer. Seçim bildirilmezse koyu temada her
     * açılışta açık renkli bir parlama görünür. Android 12 öncesinde bunun bir yolu yoktur.
     */
    private fun tellSystemTheme(theme: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (theme) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        handleVolumeKey(event) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        handleVolumeKey(event) || super.onKeyUp(keyCode, event)

    private fun handleVolumeKey(event: KeyEvent): Boolean =
        container.volumeKeyDispatcher.onKeyEvent(event.keyCode, event.action, event.repeatCount)
}
