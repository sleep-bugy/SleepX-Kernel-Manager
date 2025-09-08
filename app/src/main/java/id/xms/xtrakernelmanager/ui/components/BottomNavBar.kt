package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 1.dp), // Minimal vertical padding
        contentAlignment = Alignment.BottomCenter
    ) {
        SuperGlassCard(
            modifier = Modifier.fillMaxWidth(),
            glassIntensity = GlassIntensity.Heavy,
            onClick = null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp), // Much smaller vertical padding
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = current == screen.lowercase()
                    val (icon, color) = getIconAndColor(screen)

                    BottomNavItem(
                        icon = icon,
                        label = screen,
                        color = color,
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.lowercase()) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Animations
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f, // Reduced scale for more compact design
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_animation"
    )

    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else color.copy(alpha = 0.6f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "icon_tint_animation"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "text_color_animation"
    )

    // Background animation for the green shape
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "background_animation"
    )

    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Main container with green background shape when selected
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = backgroundAlpha * 0.9f),
                                Color(0xFF66BB6A).copy(alpha = backgroundAlpha * 0.7f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = 35f
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = iconTint
            )
        }

        // Blue capsule indicator at the bottom when selected
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        Color(0xFF2196F3).copy(alpha = backgroundAlpha)
                    } else {
                        Color.Transparent
                    }
                )
        )

        // Label text
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = textColor,
            maxLines = 1
        )
    }
}

// Helper function to get icon and color for each nav item
private fun getIconAndColor(screen: String): Pair<ImageVector, Color> {
    return when (screen.lowercase()) {
        "home" -> Pair(
            Icons.Default.Home,
            Color(0xFF4CAF50) // Green
        )
        "tuning" -> Pair(
            Icons.Default.Build,
            Color(0xFF2196F3) // Blue
        )
        "misc" -> Pair(
            Icons.Default.MiscellaneousServices,
            Color(0xFFFF9800) // Orange
        )
        "info" -> Pair(
            Icons.Default.Info,
            Color(0xFF9C27B0) // Purple
        )
        else -> Pair(
            Icons.Default.Home,
            Color(0xFF757575) // Gray
        )
    }
}