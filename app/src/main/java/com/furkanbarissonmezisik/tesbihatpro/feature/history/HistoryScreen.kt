package com.furkanbarissonmezisik.tesbihatpro.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.DetailTopBar

@Composable
fun HistoryRoute(onBack: () -> Unit) {
    HistoryScreen(onBack = onBack)
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            DetailTopBar(title = stringResource(R.string.screen_history), onBack = onBack)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
