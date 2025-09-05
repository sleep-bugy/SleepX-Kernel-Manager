package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

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
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // Add some floating blur elements for Android 16-like aesthetic
        FloatingBlurElements()
        content()
    }
}

@Composable
private fun BoxScope.FloatingBlurElements() {
    // Top left blur element
    Box(
        modifier = Modifier
            .size(200.dp)
            .offset((-50).dp, (-50).dp)
            .clip(RoundedCornerShape(100.dp))
            .blur(40.dp, BlurredEdgeTreatment.Unbounded)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
    )

    // Bottom right blur element
    Box(
        modifier = Modifier
            .size(150.dp)
            .offset(50.dp, 50.dp)
            .align(androidx.compose.ui.Alignment.BottomEnd)
            .clip(RoundedCornerShape(75.dp))
            .blur(30.dp, BlurredEdgeTreatment.Unbounded)
            .background(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
            )
    )
}

@Composable
fun SuperGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glassIntensity: GlassIntensity = GlassIntensity.Medium,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Minimal glassmorphism parameters for maximum content visibility
    val glassParams = when (glassIntensity) {
        GlassIntensity.Light -> GlassParams(0.dp, 0.4f, 0.5f, 0.2f)  // No blur, high opacity
        GlassIntensity.Medium -> GlassParams(1.dp, 0.35f, 0.45f, 0.3f)  // Minimal blur
        GlassIntensity.Heavy -> GlassParams(2.dp, 0.3f, 0.4f, 0.4f)    // Very light blur
    }

    val cardModifier = modifier
        .clip(RoundedCornerShape(28.dp))
        // Tambahkan border untuk CPU card utama
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            shape = RoundedCornerShape(28.dp)
        )
        // Only apply blur if radius > 0
        .let { mod ->
            if (glassParams.blurRadius > 0.dp) {
                mod.blur(glassParams.blurRadius, BlurredEdgeTreatment.Rectangle)
            } else {
                mod
            }
        }
        .background(
            // Gunakan background yang lebih sederhana tanpa gradient vertikal yang kompleks
            MaterialTheme.colorScheme.surface.copy(alpha = glassParams.backdropAlpha + 0.1f)
        )
        // Hilangkan inner reflections yang kompleks
        .drawWithContent {
            drawContent()
        }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 24.dp,
                pressedElevation = 32.dp,
                hoveredElevation = 28.dp
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .drawWithContent {
                        drawContent()
                        // Subtle inner shadow for depth
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 50.dp.toPx()
                            ),
                            size = Size(size.width, 50.dp.toPx())
                        )
                    },
                content = content
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 24.dp
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .drawWithContent {
                        drawContent()
                        // Subtle inner shadow for depth
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 50.dp.toPx()
                            ),
                            size = Size(size.width, 50.dp.toPx())
                        )
                    },
                content = content
            )
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
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .blur(18.dp, BlurredEdgeTreatment.Unbounded)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            ),
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}
