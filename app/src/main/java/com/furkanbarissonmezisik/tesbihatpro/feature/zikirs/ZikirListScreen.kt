package com.furkanbarissonmezisik.tesbihatpro.feature.zikirs

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
fun ZikirListRoute(onBack: () -> Unit) {
    ZikirListScreen(onBack = onBack)
}

@Composable
fun ZikirListScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            DetailTopBar(title = stringResource(R.string.screen_zikirs), onBack = onBack)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
