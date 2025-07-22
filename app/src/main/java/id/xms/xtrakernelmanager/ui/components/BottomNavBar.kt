package id.xms.xtrakernelmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val selectedRoute = currentRoute?.destination?.route ?: "home"

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