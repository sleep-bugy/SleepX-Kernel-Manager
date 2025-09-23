package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.BatteryStats

// This function will format the duration from seconds to a readable string.
private fun formatSotDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "${hours}h ${minutes}m ${secs}s"
}

@Composable
fun InfoCard(
    batteryStats: BatteryStats,
    modifier: Modifier = Modifier,
) {
    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Statistik Penggunaan Baterai", style = MaterialTheme.typography.titleMedium)
            // Use the new screenOnTimeInSeconds property with the formatter
            Text(
                "Screen On Time: ${formatSotDuration(batteryStats.screenOnTimeInSeconds)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Aplikasi dengan konsumsi daya tertinggi:", style = MaterialTheme.typography.bodyMedium)

            if (batteryStats.topApps.isNotEmpty()) {
                RealtimeGraph(
                    data = batteryStats.topApps.map { it.usagePercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No battery usage data available.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun RealtimeGraph(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gradientFill: Brush = Brush.verticalGradient(
        colors = listOf(
            lineColor.copy(alpha = 0.3f),
            Color.Transparent
        )
    )
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Waiting for data...", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Canvas(modifier = modifier) {
        val maxValue = data.maxOrNull() ?: 0f
        val minValue = 0f
        val valueRange = if (maxValue > minValue) maxValue - minValue else 1f

        val path = Path()
        path.moveTo(0f, size.height * (1 - (data.first() - minValue) / valueRange))

        data.forEachIndexed { index, value ->
            val x = if (data.size > 1) index.toFloat() / (data.size - 1) * size.width else 0f
            val y = size.height * (1 - ((value - minValue) / valueRange).coerceIn(0f, 1f))
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f)
        )

        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        drawPath(
            path = path,
            brush = gradientFill
        )
    }
}
