package id.xms.xtrakernelmanager.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.AppUsage
import id.xms.xtrakernelmanager.data.model.BatteryStats

@Composable
fun BatteryUsageCard(
    batteryStats: BatteryStats,
    modifier: Modifier = Modifier,
) {
    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery Usage",
                    style = MaterialTheme.typography.titleLarge
                )
                if (batteryStats.batteryLevelHistory.isNotEmpty()) {
                    Text(
                        text = "${batteryStats.batteryLevelHistory.last().toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            BatteryLevelGraph(
                history = batteryStats.batteryLevelHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Column {
                Text("Screen On Time", style = MaterialTheme.typography.titleMedium)
                Text(formatDuration(batteryStats.screenOnTimeInSeconds), style = MaterialTheme.typography.bodyLarge)
            }
            AppConsumptionList(batteryStats.topApps)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "${hours}h ${minutes}m ${secs}s"
}

@Composable
private fun BatteryLevelGraph(
    history: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val gradientFill = Brush.verticalGradient(
        colors = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent)
    )

    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

        val path = Path()
        val stepX = size.width / (history.size - 1)

        path.moveTo(0f, size.height * (1 - history.first() / 100f))

        for (i in 1 until history.size) {
            val x = i * stepX
            val y = size.height * (1 - history[i] / 100f)
            path.lineTo(x, y)
        }

        drawPath(path, color = lineColor, style = Stroke(width = 5f))

        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        drawPath(path, brush = gradientFill)
    }
}

@Composable
private fun AppConsumptionList(apps: List<AppUsage>) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App Consumption", style = MaterialTheme.typography.titleMedium)
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No app usage data available.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            apps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    app.icon?.let {
                        Canvas(modifier = Modifier.size(32.dp)) {
                            drawIntoCanvas { canvas ->
                                it.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                                it.draw(canvas.nativeCanvas)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format("%.1f", app.usagePercent)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
