package id.xms.xtrakernelmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import id.xms.xtrakernelmanager.R

@Composable
fun BottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val selectedRoute = currentRoute?.destination?.route ?: "home" // Default ke home kalau null

    NavigationBar(
        modifier = modifier.haze(
            state = TODO()
        )
            .hazeChild(
                state = TODO(),
                style = HazeStyle(
                    tint = null, // Explicitly provide null for the tint parameter
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    blurRadius = 10.dp
                ))
        ,
        containerColor = Color.Transparent // Biar haze dominan
    ) {
        NavigationBarItem(
            selected = selectedRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.semantics { contentDescription = "Home Tab" }
                )
            },
            label = { Text("Home", style = MaterialTheme.typography.bodySmall) }
        )
        NavigationBarItem(
            selected = selectedRoute == "tuning",
            onClick = {
                navController.navigate("tuning") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Tuning",
                    modifier = Modifier.semantics { contentDescription = "Tuning Tab" }
                )
            },
            label = { Text("Tuning", style = MaterialTheme.typography.bodySmall) }
        )
        NavigationBarItem(
            selected = selectedRoute == "terminal",
            onClick = {
                navController.navigate("terminal") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal",
                    modifier = Modifier.semantics { contentDescription = "Terminal Tab" }
                )
            },
            label = { Text("Terminal", style = MaterialTheme.typography.bodySmall) }
        )
        NavigationBarItem(
            selected = selectedRoute == "info",
            onClick = {
                navController.navigate("info") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.semantics { contentDescription = "Info Tab" }
                )
            },
            label = { Text("Info", style = MaterialTheme.typography.bodySmall) }
        )
    }
}