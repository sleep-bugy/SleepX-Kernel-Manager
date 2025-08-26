package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.R.*
import id.xms.xtrakernelmanager.R.string.home

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .shadow(elevation = 32.dp, shape = RoundedCornerShape(40.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .height(90.dp),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        items.forEach { screen ->
            val icon = when {
                screen.equals(stringResource(R.string.home), ignoreCase = true)   -> Icons.Default.Home
                screen.equals(stringResource(R.string.tuning), ignoreCase = true) -> Icons.Default.Build
                screen.equals(stringResource(R.string.misc), ignoreCase = true) -> Icons.Default.MiscellaneousServices
                screen.equals(stringResource(R.string.info), ignoreCase = true)   -> Icons.Default.Info
                else     -> Icons.Default.Home
            }
            NavigationBarItem(
                icon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp) // Ukuran ikon lebih besar
                            .clip(RoundedCornerShape(28.dp)) // Rounded shape lebih besar
                            .background(if (current == screen.lowercase()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent) // Highlight aktif Material 3 You dengan alpha atau transparan
                            .padding(10.dp) // Padding ikon lebih besar
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen,
                            modifier = Modifier.size(28.dp), // Ukuran ikon di dalam Box lebih besar
                            tint = if (current == screen.lowercase()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant // Ikon Material 3 You
                        )
                    }
                },
                label = { Text(screen, fontSize = 14.sp) }, // Ukuran teks label lebih besar
                selected = current == screen.lowercase(),
                onClick = {
                    navController.navigate(screen.lowercase()) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f) // Indikator dibuat transparan karena Box sudah menghandle background
                )
            )
        }
    }
}