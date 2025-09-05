package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ultra-Strong Glassmorphism Surface - The most powerful glass effect
 */
@Composable
fun UltraGlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Pre-calculate colors in @Composable context
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            // Triple-layer blur for maximum frosted glass effect
            .blur(35.dp, BlurredEdgeTreatment.Unbounded)
            .drawBehind {
                // Outer atmospheric glow
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color.Cyan.copy(alpha = 0.15f) else Color.Blue.copy(alpha = 0.08f),
                            if (isDark) Color.Magenta.copy(alpha = 0.12f) else Color(0xFF9C27B0).copy(alpha = 0.06f), // Material Purple
                            Color.Transparent
                        ),
                        radius = size.maxDimension * 1.2f
                    ),
                    cornerRadius = CornerRadius(36.dp.toPx())
                )
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        // Center glass highlight
                        surfaceColor.copy(alpha = 0.25f),
                        // Mid-range glass body
                        surfaceColor.copy(alpha = 0.08f),
                        // Edge with subtle color tint
                        primaryContainerColor.copy(alpha = 0.12f),
                        // Outer edge fade
                        surfaceColor.copy(alpha = 0.05f)
                    ),
                    radius = 800f
                )
            )
            // Secondary blur layer for ultra frosted effect
            .blur(20.dp, BlurredEdgeTreatment.Rectangle)
            // Multi-gradient border for prism-like edge
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.Cyan.copy(alpha = 0.4f),
                        Color.Magenta.copy(alpha = 0.3f),
                        Color.Yellow.copy(alpha = 0.2f),
                        Color.Blue.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            // Final blur for atmospheric effect
            .blur(8.dp, BlurredEdgeTreatment.Rectangle)
            // Complex inner reflections
            .drawWithContent {
                drawContent()

                // Multiple glass reflection layers
                // Top-left diagonal reflection
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.6f, size.height * 0.6f)
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx()),
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
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    size = size
                )

                // Bottom-right subtle glow
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.1f)
                        ),
                        radius = size.maxDimension * 0.5f,
                        center = Offset(size.width * 0.8f, size.height * 0.8f)
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    size = size
                )
            },
        content = content
    )
}

/**
 * Glassmorphism Container with intense depth effect
 */
@Composable
fun DeepGlassContainer(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    UltraGlassSurface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .drawBehind {
                            // Text glow effect
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.1f),
                                        Color.Transparent
                                    ),
                                    radius = size.maxDimension * 0.8f
                                ),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                )
            }

            Column(content = content)
        }
    }
}

/**
 * Floating Glass Panel with extreme blur
 */
@Composable
fun FloatingGlassPanel(
    modifier: Modifier = Modifier,
    elevation: Float = 32f,
    content: @Composable BoxScope.() -> Unit
) {
    // Pre-calculate colors in @Composable context
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            // Extreme blur for floating effect
            .blur((elevation * 0.8f).dp, BlurredEdgeTreatment.Unbounded)
            .drawBehind {
                // Floating shadow with color
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.2f),
                            secondaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = size.maxDimension * 1.1f
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    topLeft = Offset(-8.dp.toPx(), -8.dp.toPx()),
                    size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx())
                )
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.15f),
                        surfaceColor.copy(alpha = 0.06f),
                        surfaceVariantColor.copy(alpha = 0.08f),
                        surfaceColor.copy(alpha = 0.12f)
                    )
                )
            )
            // Additional atmospheric blur
            .blur(12.dp, BlurredEdgeTreatment.Rectangle)
            // Prismatic border
            .border(
                width = 1.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.Cyan.copy(alpha = 0.3f),
                        Color.Magenta.copy(alpha = 0.2f),
                        Color.Yellow.copy(alpha = 0.15f),
                        Color.Blue.copy(alpha = 0.25f),
                        Color.Green.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            // Inner glass reflections
            .drawWithContent {
                drawContent()

                // Curved glass reflection
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.7f, size.height * 0.7f)
                    ),
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    size = Size(size.width * 0.7f, size.height * 0.7f)
                )

                // Edge light refraction
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startX = size.width * 0.8f,
                        endX = size.width
                    ),
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    topLeft = Offset(size.width * 0.8f, 0f),
                    size = Size(size.width * 0.2f, size.height)
                )
            },
        content = content
    )
}

/**
 * Glass Morphism Background Overlay
 */
@Composable
fun GlassmorphismOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    // Pre-calculate colors in @Composable context
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier
            .blur((25f * intensity).dp, BlurredEdgeTreatment.Unbounded)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.1f * intensity),
                        primaryContainerColor.copy(alpha = 0.05f * intensity),
                        secondaryContainerColor.copy(alpha = 0.03f * intensity),
                        Color.Transparent
                    ),
                    radius = 1000f
                )
            )
            .blur((12f * intensity).dp, BlurredEdgeTreatment.Rectangle),
        content = content
    )
}
