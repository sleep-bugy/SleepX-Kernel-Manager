package id.xms.xtrakernelmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            val route = screen.lowercase()
            val selected = current == route
            val (icon, label) = when (route) {
                "home" -> Pair(Icons.Filled.Home, "Home")
                "tuning" -> Pair(Icons.Filled.Build, "Tuning")
                "misc" -> Pair(Icons.Filled.MiscellaneousServices, "Misc")
                "info" -> Pair(Icons.Filled.Info, "Info")
                else -> Pair(Icons.AutoMirrored.Filled.ArrowBack, screen)
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White
                )
            )
        }
    }
}
