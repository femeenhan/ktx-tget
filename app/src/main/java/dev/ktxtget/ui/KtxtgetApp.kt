package dev.ktxtget.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ktxtget.R
import dev.ktxtget.data.MacroPreferencesRepository

private object Routes {
    const val MAIN: String = "main"
    const val LOG: String = "log"
}

@Composable
fun KtxtgetApp(repository: MacroPreferencesRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination?.hierarchy?.any { it.route == Routes.MAIN } == true,
                    onClick = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.nav_main)) },
                )
                NavigationBarItem(
                    selected = destination?.hierarchy?.any { it.route == Routes.LOG } == true,
                    onClick = {
                        navController.navigate(Routes.LOG) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.nav_log)) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.MAIN) {
                MainScreen(repository = repository)
            }
            composable(Routes.LOG) {
                LogScreen()
            }
        }
    }
}
