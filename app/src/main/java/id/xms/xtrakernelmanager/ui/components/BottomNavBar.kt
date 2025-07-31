package id.xms.xtrakernelmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import id.xms.xtrakernelmanager.R

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            val icon = when (screen) {
                stringResource(R.string.home)   -> Icons.Default.Home
                stringResource(R.string.tuning) -> Icons.Default.Build
                stringResource(R.string.terminal) -> Icons.Default.Terminal
                stringResource(R.string.info)   -> Icons.Default.Info
                else     -> Icons.Default.Home
            }
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = screen) },
                label = { Text(screen) },
                selected = current == screen.lowercase(),
                onClick = {
                    navController.navigate(screen.lowercase()) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}