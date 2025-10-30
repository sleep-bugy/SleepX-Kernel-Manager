package id.xms.xtrakernelmanager.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import id.xms.xtrakernelmanager.data.model.ThemeType

// Dark theme color palette - optimized for OLED displays (GlassMorphism)
private val GlassMorphismColorScheme = darkColorScheme(
    // FKM-like layout with distinct SleepX Cyan palette
    primary = Color(0xFF00BCD4),        // cyan
    onPrimary = Color(0xFF0E1116),
    primaryContainer = Color(0xFF004D57),
    onPrimaryContainer = Color(0xFFCCF7FF),

    secondary = Color(0xFF80CBC4),     // teal
    onSecondary = Color(0xFF0E1116),
    secondaryContainer = Color(0xFF255D57),
    onSecondaryContainer = Color(0xFFE1F7F4),

    tertiary = Color(0xFFFFC107),      // amber accent
    onTertiary = Color(0xFF0E1116),
    tertiaryContainer = Color(0xFF5A4400),
    onTertiaryContainer = Color(0xFFFFF0C2),

    error = Color(0xFFEF5350),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),

    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6E8EC),

    surface = Color(0xFF121620),
    onSurface = Color(0xFFE6E8EC),
    surfaceVariant = Color(0xFF1A2030),
    onSurfaceVariant = Color(0xFFAAB2C0),

    outline = Color(0xFF3A4354),
    outlineVariant = Color(0xFF2B3342),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E8EC),
    inverseOnSurface = Color(0xFF121620),
    inversePrimary = Color(0xFF6C9BFF),

    surfaceDim = Color(0xFF0F131B),
    surfaceBright = Color(0xFF202637),
    surfaceContainerLowest = Color(0xFF0B0E13),
    surfaceContainerLow = Color(0xFF141A26),
    surfaceContainer = Color(0xFF171D2A),
    surfaceContainerHigh = Color(0xFF1B2232),
    surfaceContainerHighest = Color(0xFF1F2740)
)

// Pure AMOLED theme - true black backgrounds for maximum battery savings
private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF00BCD4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFC8E6C9),

    tertiary = Color(0xFFFFAB91),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFF7043),
    onTertiaryContainer = Color(0xFFFFCCBC),

    error = Color(0xFFEF5350),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFCDD2),

    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E8EC),

    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E8EC),
    surfaceVariant = Color(0xFF102027),
    onSurfaceVariant = Color(0xFFB0BEC5),

    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF2C2C2C),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF1976D2),

    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF0F0F0F),
    surfaceContainerHigh = Color(0xFF151515),
    surfaceContainerHighest = Color(0xFF1A1A1A)
)

@Composable
fun XtraTheme(
    themeType: ThemeType = ThemeType.GLASSMORPHISM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (themeType) {
        ThemeType.GLASSMORPHISM -> GlassMorphismColorScheme
        ThemeType.AMOLED -> AmoledColorScheme
        ThemeType.MATERIAL3 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Use dynamic colors for Android 12+ (follows wallpaper)
                dynamicDarkColorScheme(context)
            } else {
                // Fallback to Material 3 dark colors for older Android versions
                darkColorScheme()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SleepXShapes,
        content = content
    )
}
