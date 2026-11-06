package com.furkanbarissonmezisik.tesbihatpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.furkanbarissonmezisik.tesbihatpro.core.ui.theme.TesbihatProTheme
import com.furkanbarissonmezisik.tesbihatpro.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TesbihatProTheme {
                AppNavHost()
            }
        }
    }
}
