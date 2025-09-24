package id.xms.xtrakernelmanager.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.data.model.AppUsage
import id.xms.xtrakernelmanager.data.model.BatteryStats
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.nativeCanvas


@Composable
fun BatteryUsageCard(
    batteryStats: BatteryStats,
    sotCached: Long,
    onRefreshScreenOnTime: suspend () -> Long,
    modifier: Modifier = Modifier,
) {
    var screenOnTimeSeconds by remember { mutableLongStateOf(sotCached) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(sotCached) { screenOnTimeSeconds = sotCached }

    SuperGlassCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderRow(batteryStats)
            BatteryGraphToggleable(
                history = batteryStats.batteryLevelHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            ScreenOnTimeRow(
                seconds = screenOnTimeSeconds,
                onRefresh = { scope.launch { screenOnTimeSeconds = onRefreshScreenOnTime() } }
            )
            AppConsumptionList(batteryStats.topApps)
        }
    }
}

/* ------------------ HEADER ------------------ */
@Composable
private fun HeaderRow(stats: BatteryStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Battery Usage", style = MaterialTheme.typography.titleLarge)
        if (stats.batteryLevelHistory.isNotEmpty()) {
            Text(
                text = "${stats.batteryLevelHistory.last().toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* ------------------ TOGGLEABLE GRAPH ------------------ */
@Composable
private fun BatteryGraphToggleable(
    history: List<Float>,
    modifier: Modifier = Modifier
) {

    if (history.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Collecting battery data…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }
    var isLine by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        /* Toggle chip */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilterChip(
                selected = isLine,
                onClick = { isLine = true },
                label = { Text("Line") },
                leadingIcon = {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !isLine,
                onClick = { isLine = false },
                label = { Text("Bar") },
                leadingIcon = {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        /* Animasi cross-fade + scale */
        AnimatedContent(
            targetState = isLine,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.9f))
                    .togetherWith(fadeOut() + scaleOut(targetScale = 0.9f))
            },
            label = "graphToggle"
        ) { line ->
            if (line) LineChart(history) else BarChart(history)
        }
    }
}

/* ------------------ LINE CHART ------------------ */
@Composable
private fun LineChart(history: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = lineColor.copy(alpha = 0.12f)

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val pad = 8.dp.toPx(); val chartH = h - 2 * pad
        val validCount = history.size.coerceAtLeast(2)
        val stepX = size.width / (validCount - 1).toFloat()
        drawGrid(pad, gridColor, w, h)
        drawLabels(pad, gridColor, h)

        val path = Path()
        val grad = Brush.verticalGradient(
            0f to lineColor.copy(alpha = 0.5f),
            1f to Color.Transparent,
            startY = pad, endY = h - pad
        )
        history.take(validCount).forEachIndexed { i, lvl ->
            val x = i * stepX
            val y = pad + (1 - lvl / 100f) * chartH
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f))
        val fill = Path().apply {
            addPath(path); lineTo(w, h - pad); lineTo(0f, h - pad); close()
        }
        drawPath(fill, brush = grad)
        if (history.size < 2) {
            drawContext.canvas.nativeCanvas.drawText(
                "1 data point",
                8.dp.toPx(),
                pad + 12.sp.toPx(),
                android.graphics.Paint().apply {
                    color = lineColor.toArgb()
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                }
            )
        }
    }
}

/* ------------------ BAR CHART ------------------ */
@Composable
private fun BarChart(history: List<Float>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = barColor.copy(alpha = 0.12f)

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val pad = 8.dp.toPx(); val chartH = h - 2 * pad
        val validCount = history.size.coerceAtLeast(2)   // <-- sudah ada
        val barW = size.width / validCount.toFloat()
        val batangW = (barW * 0.8f).coerceAtLeast(4.dp.toPx())   // <-- minimal 4 dp

        drawGrid(pad, gridColor, w, h)
        drawLabels(pad, gridColor, h)

        history.take(validCount).forEachIndexed { i, lvl ->
            val left = i * barW
            val top = pad + (1 - lvl / 100f) * chartH
            drawRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(batangW, chartH * (lvl / 100f))
            )

            if (history.size < 2) {
                drawContext.canvas.nativeCanvas.drawText(
                    "1 data point",
                    8.dp.toPx(),
                    pad + 12.sp.toPx(),
                    android.graphics.Paint().apply {
                        color = barColor.toArgb()
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

/* ------------------ HELPER DRAW ------------------ */
private fun DrawScope.drawGrid(pad: Float, gridColor: Color, w: Float, h: Float) {
    drawLine(gridColor, Offset(0f, pad), Offset(w, pad), strokeWidth = 1f)
    drawLine(gridColor, Offset(0f, h - pad), Offset(w, h - pad), strokeWidth = 1f)
}

private fun DrawScope.drawLabels(pad: Float, gridColor: Color, h: Float) {
    val paint = android.graphics.Paint().apply {
        color = gridColor.toArgb(); textSize = 10.sp.toPx(); isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.apply {
        drawText("100%", 4f, pad - 2f, paint)
        drawText("0%", 4f, h - pad + 12f, paint)
    }
}

/* ------------------ SCREEN-ON TIME ------------------ */
@Composable
private fun ScreenOnTimeRow(
    seconds: Long,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Screen On Time", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDuration(seconds),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Tap refresh for latest data",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        FilledTonalButton(
            onClick = onRefresh,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Refresh")
        }
    }
}

/* ------------------ APP CONSUMPTION ------------------ */
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
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) { Text("Grant Permission") }
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
                            it.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                            drawIntoCanvas { canvas -> it.draw(canvas.nativeCanvas) }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "%.1f%%".format(app.usagePercent),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/* ------------------ FORMAT DURATION ------------------ */
private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "${h}h ${m}m ${s}s"
}
