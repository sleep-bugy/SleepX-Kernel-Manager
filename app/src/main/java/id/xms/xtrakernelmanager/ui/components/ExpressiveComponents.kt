package id.xms.xtrakernelmanager.ui.components

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
import id.xms.xtrakernelmanager.ui.theme.BlurSurfaceColors

@Composable
fun BlurCard(
    modifier: Modifier = Modifier,
    blurLevel: BlurLevel = BlurLevel.Heavy, // Default ke Heavy untuk efek maksimal
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Intensitas blur yang diperkuat untuk glassmorphism
    val (blurRadius, secondaryBlur, glowRadius) = when (blurLevel) {
        BlurLevel.Light -> Triple(15.dp, 8.dp, 0.3f)
        BlurLevel.Medium -> Triple(25.dp, 12.dp, 0.5f)
        BlurLevel.Heavy -> Triple(35.dp, 18.dp, 0.8f)
    }

    // Warna background dengan transparansi yang diperkuat
    val backgroundColor = when (blurLevel) {
        BlurLevel.Light -> if (isDark) BlurSurfaceColors.darkLightBlur else BlurSurfaceColors.lightBlur
        BlurLevel.Medium -> if (isDark) BlurSurfaceColors.darkMediumBlur else BlurSurfaceColors.mediumBlur
        BlurLevel.Heavy -> if (isDark) BlurSurfaceColors.darkHeavyBlur else BlurSurfaceColors.heavyBlur
    }

    val cardModifier = modifier
        .clip(RoundedCornerShape(28.dp))
        // Outer atmospheric glow
        .drawBehind {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6750A4).copy(alpha = glowRadius * 0.15f),
                        Color(0xFF625B71).copy(alpha = glowRadius * 0.1f),
                        Color.Transparent
                    ),
                    radius = size.maxDimension * 1.2f
                ),
                cornerRadius = CornerRadius(32.dp.toPx())
            )
        }
        // Primary blur layer yang diperkuat
        .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    // Top glass reflection yang lebih cerah
                    Color.White.copy(alpha = 0.25f),
                    // Mid section dengan warna utama
                    backgroundColor.copy(alpha = 0.15f),
                    // Main glass body
                    Color.White.copy(alpha = 0.08f),
                    // Bottom dengan subtle tint
                    backgroundColor.copy(alpha = 0.12f),
                    // Bottom highlight
                    Color.White.copy(alpha = 0.18f)
                )
            )
        )
        // Secondary blur untuk frosted effect
        .blur(secondaryBlur, BlurredEdgeTreatment.Rectangle)
        // Enhanced prismatic border
        .border(
            width = 1.5.dp,
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.Cyan.copy(alpha = 0.4f),
                    Color.Magenta.copy(alpha = 0.3f),
                    Color.Yellow.copy(alpha = 0.25f),
                    Color.Blue.copy(alpha = 0.35f),
                    Color.Green.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.6f)
                )
            ),
            shape = RoundedCornerShape(28.dp)
        )
        // Final atmospheric blur
        .blur(6.dp, BlurredEdgeTreatment.Rectangle)
        // Complex inner glass reflections
        .drawWithContent {
            drawContent()

            // Main diagonal reflection
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.6f, size.height * 0.6f)
                ),
                cornerRadius = CornerRadius(28.dp.toPx()),
                size = Size(size.width * 0.6f, size.height * 0.6f)
            )

            // Center spotlight reflection
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width * 0.3f, size.height * 0.2f)
                ),
                cornerRadius = CornerRadius(28.dp.toPx())
            )

            // Edge light refraction
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    startX = size.width * 0.85f,
                    endX = size.width
                ),
                cornerRadius = CornerRadius(28.dp.toPx()),
                topLeft = Offset(size.width * 0.85f, 0f),
                size = Size(size.width * 0.15f, size.height)
            )
        }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = when (blurLevel) {
                    BlurLevel.Light -> 16.dp
                    BlurLevel.Medium -> 24.dp
                    BlurLevel.Heavy -> 32.dp
                },
                pressedElevation = when (blurLevel) {
                    BlurLevel.Light -> 20.dp
                    BlurLevel.Medium -> 28.dp
                    BlurLevel.Heavy -> 36.dp
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .drawWithContent {
                        drawContent()
                        // Subtle inner shadow untuk depth
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 40.dp.toPx()
                            ),
                            size = Size(size.width, 40.dp.toPx())
                        )
                    },
                content = content
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = when (blurLevel) {
                    BlurLevel.Light -> 16.dp
                    BlurLevel.Medium -> 24.dp
                    BlurLevel.Heavy -> 32.dp
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .drawWithContent {
                        drawContent()
                        // Subtle inner shadow untuk depth
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 40.dp.toPx()
                            ),
                            size = Size(size.width, 40.dp.toPx())
                        )
                    },
                content = content
            )
        }
    }
}

@Composable
fun GlassmorphismSurface(
    modifier: Modifier = Modifier,
    blurRadius: Float = 0f,  // No blur at all
    alpha: Float = 0.5f,     // High opacity for visibility
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))  // Smaller corner radius
            // No blur applied for maximum visibility
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = alpha + 0.2f),
                        MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.9f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),  // Reduced padding
        content = content
    )
}

@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.filledTonalButtonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        content = content
    )
}

@Composable
fun ExpressiveFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 16.dp,
            pressedElevation = 20.dp
        ),
        content = content
    )
}

enum class BlurLevel {
    Light,
    Medium,
    Heavy
}
