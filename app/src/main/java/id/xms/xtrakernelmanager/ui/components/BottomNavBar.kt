package id.xms.xtrakernelmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        NavigationBarItem(
            selected = selectedRoute == "home",
            onClick = { navController.navigate("home") { launchSingleTop = true } },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        )
        NavigationBarItem(
            selected = selectedRoute == "tuning",
            onClick = { navController.navigate("tuning") { launchSingleTop = true } },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Tuning") },
            label = { Text("Tuning", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        )
        NavigationBarItem(
            selected = selectedRoute == "terminal",
            onClick = { navController.navigate("terminal") { launchSingleTop = true } },
            icon = { Icon(imageVector = Icons.Default.Terminal, contentDescription = "Terminal") },
            label = { Text("Terminal", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        )
        NavigationBarItem(
            selected = selectedRoute == "info",
            onClick = { navController.navigate("info") { launchSingleTop = true } },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Info") },
            label = { Text("Info", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        )
    }
}