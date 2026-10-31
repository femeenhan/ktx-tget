package dev.ktxtget.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.ui.theme.Sage

private object Routes {
    const val MAIN: String = "main"
    const val LOG: String = "log"
    const val GUIDE: String = "guide"
}

@Composable
fun KtxtgetApp(repository: MacroPreferencesRepository) {
    val navController = rememberNavController()
    Scaffold(containerColor = Sage) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier
                .padding(innerPadding)
                .imePadding(),
        ) {
            composable(Routes.MAIN) {
                MainScreen(
                    repository = repository,
                    onOpenLog = {
                        navController.navigate(Routes.LOG) {
                            launchSingleTop = true
                        }
                    },
                    onOpenGuide = {
                        navController.navigate(Routes.GUIDE) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.LOG) {
                LogScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.GUIDE) {
                GuideScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
