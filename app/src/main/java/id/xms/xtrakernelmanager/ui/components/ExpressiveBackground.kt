package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

// Enum for glass intensity levels
enum class GlassIntensity {
    Light,
    Medium,
    Heavy
}

// Data class to hold glassmorphism parameters
data class GlassParams(
    val blurRadius: androidx.compose.ui.unit.Dp,
    val backdropAlpha: Float,
    val borderAlpha: Float,
    val glowIntensity: Float
)

@Composable
fun ExpressiveBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic animated background for dark theme
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawExpressiveBackground(isDark, size.width, size.height)
        }

        // Content overlay
        content()
    }
}

private fun DrawScope.drawExpressiveBackground(
    isDark: Boolean,
    width: Float,
    height: Float
) {
    if (isDark) {
        // Dark theme: Subtle gradient with animated particles
        val darkGradient = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0A0E13), // Very dark blue-gray
                Color(0xFF121212), // Pure dark
                Color(0xFF0D1117)  // GitHub dark
            ),
            center = Offset(width * 0.3f, height * 0.2f),
            radius = width * 0.8f
        )

        drawRect(
            brush = darkGradient,
            size = androidx.compose.ui.geometry.Size(width, height)
        )

        // Add subtle animated dots/particles
        repeat(12) { index ->
            val x = (width * 0.1f) + (index % 4) * (width * 0.25f) + (Random.nextFloat() * 50)
            val y = (height * 0.1f) + (index / 4) * (height * 0.3f) + (Random.nextFloat() * 50)
            val radius = 2f + Random.nextFloat() * 3f

            drawCircle(
                color = Color(0xFF90CAF9).copy(alpha = 0.1f + Random.nextFloat() * 0.1f),
                radius = radius,
                center = Offset(x, y)
            )
        }

        // Add subtle connecting lines
        repeat(6) { index ->
            val startX = width * 0.2f + index * (width * 0.15f)
            val startY = height * 0.3f + sin(index * 0.5f) * height * 0.1f
            val endX = startX + width * 0.1f
            val endY = startY + height * 0.2f

            drawLine(
                color = Color(0xFF81C784).copy(alpha = 0.05f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }
    } else {
        // Light theme fallback (though this won't be used in dark-only mode)
        drawRect(
            color = Color(0xFFFAFAFA),
            size = androidx.compose.ui.geometry.Size(width, height)
        )
    }
}

@Composable
fun SuperGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glassIntensity: GlassIntensity = GlassIntensity.Medium,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

// Keep the original GlassCard for backward compatibility
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SuperGlassCard(
        modifier = modifier,
        onClick = onClick,
        glassIntensity = GlassIntensity.Heavy,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
