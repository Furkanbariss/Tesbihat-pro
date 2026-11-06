package com.furkanbarissonmezisik.tesbihatpro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.furkanbarissonmezisik.tesbihatpro.feature.counter.CounterRoute
import com.furkanbarissonmezisik.tesbihatpro.feature.history.HistoryRoute
import com.furkanbarissonmezisik.tesbihatpro.feature.settings.SettingsRoute
import com.furkanbarissonmezisik.tesbihatpro.feature.zikirs.ZikirListRoute

private object Routes {
    const val COUNTER = "counter"
    const val ZIKIRS = "zikirs"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.COUNTER,
        modifier = modifier,
    ) {
        composable(Routes.COUNTER) {
            CounterRoute(
                onOpenZikirs = { navController.open(Routes.ZIKIRS) },
                onOpenHistory = { navController.open(Routes.HISTORY) },
                onOpenSettings = { navController.open(Routes.SETTINGS) },
            )
        }
        composable(Routes.ZIKIRS) {
            ZikirListRoute(onBack = { navController.navigateUp() })
        }
        composable(Routes.HISTORY) {
            HistoryRoute(onBack = { navController.navigateUp() })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { navController.navigateUp() })
        }
    }
}

/** Çift dokunuşta aynı ekranın yığına iki kez girmesini önler. */
private fun NavController.open(route: String) {
    navigate(route) { launchSingleTop = true }
}
