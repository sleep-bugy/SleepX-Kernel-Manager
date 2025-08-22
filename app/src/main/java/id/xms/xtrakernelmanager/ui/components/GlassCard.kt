package id.xms.xtrakernelmanager.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun GlassCard(
    isGlassEffectEnabled: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    glassBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    solidBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    val finalBackgroundColor: Color
    val backgroundBrush: Brush
    val strokeColor = MaterialTheme.colorScheme.outline

    if (isGlassEffectEnabled) {
        finalBackgroundColor = Color.Transparent
        backgroundBrush = Brush.verticalGradient(
            listOf(
                glassBackgroundColor,
                glassBackgroundColor.copy(alpha = glassBackgroundColor.alpha * 18f)
            )
        )
    } else {
        finalBackgroundColor = solidBackgroundColor
        backgroundBrush = Brush.verticalGradient(
            listOf(
                solidBackgroundColor,
                solidBackgroundColor
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = finalBackgroundColor
        ),
        border = BorderStroke(2.dp, strokeColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = backgroundBrush,
                    shape = shape
                )
        ) {
            content()
        }
    }
}