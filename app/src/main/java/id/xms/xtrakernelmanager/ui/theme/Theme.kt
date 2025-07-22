package id.xms.xtrakernelmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XtraKernelManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        else -> if (darkTheme) darkColorScheme() else lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography.copy(
            titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), // Toolbar
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium), // Card Header
            bodyMedium = TextStyle(fontSize = 14.sp), // Regular text
            bodySmall = TextStyle(fontSize = 12.sp) // Small label (status, badge)
        ),
        shapes = MaterialTheme.shapes.copy(
            large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp) // Card radius
        ),
        content = content
    )
}