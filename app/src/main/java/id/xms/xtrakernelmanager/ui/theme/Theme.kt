package id.xms.xtrakernelmanager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark theme color palette - optimized for OLED displays
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D1B2A),
    primaryContainer = Color(0xFF1B263B),
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF0A1F0A),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC8E6C9),

    tertiary = Color(0xFFFFAB91),
    onTertiary = Color(0xFF2C1810),
    tertiaryContainer = Color(0xFF5D4037),
    onTertiaryContainer = Color(0xFFFFCCBC),

    error = Color(0xFFEF5350),
    onError = Color(0xFF1A0000),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),

    background = Color(0xFF0A0E13),
    onBackground = Color(0xFFE3E3E3),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBDBDBD),

    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE1E1E1),
    inverseOnSurface = Color(0xFF2C2C2C),
    inversePrimary = Color(0xFF1976D2),

    surfaceDim = Color(0xFF0F0F0F),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLowest = Color(0xFF090909),
    surfaceContainerLow = Color(0xFF191919),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E)
)

@Composable
fun XtraTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Force dark theme always - ignore system theme
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = id.xms.xtrakernelmanager.ui.theme.Typography,
        content = content
    )
}