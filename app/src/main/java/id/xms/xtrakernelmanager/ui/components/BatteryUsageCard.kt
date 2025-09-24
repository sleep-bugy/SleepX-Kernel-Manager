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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                    .height(245.dp)
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

/* ------------------ LINE CHART (REFACTORED FOR LAYOUT) ------------------ */
@Composable
private fun LineChart(history: List<Float>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val gradient = Brush.verticalGradient(
        colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
    )

    // Main container for the chart and its labels
    Column(modifier = modifier) {
        // A row to hold Y-Axis labels and the chart canvas side-by-side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Take up all available vertical space
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("100%", style = MaterialTheme.typography.labelSmall)
                Text("75%", style = MaterialTheme.typography.labelSmall)
                Text("50%", style = MaterialTheme.typography.labelSmall)
                Text("25%", style = MaterialTheme.typography.labelSmall)
                Text("0%", style = MaterialTheme.typography.labelSmall)
            }

            // The actual Canvas for drawing the chart
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f) // Fill remaining horizontal space
            ) {
                // Padding is now only needed inside the canvas, not for the whole component
                val padTop = 18.dp.toPx()
                val padBottom = 18.dp.toPx()
                val chartHeight = size.height - padTop - padBottom

                // Draw subtle horizontal grid lines
                val gridLineCount = 4
                for (i in 0..gridLineCount) {
                    val y = padTop + i * (chartHeight / gridLineCount)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Create a smooth, curved path
                val linePath = Path().apply {
                    val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                    val points = history.mapIndexed { i, lvl ->
                        val x = i * stepX
                        val y = padTop + (1 - lvl / 100f) * chartHeight
                        Offset(x, y)
                    }
                    cubicSplineTo(points)
                }

                // Path for the gradient fill underneath the line
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(size.width, size.height - padBottom)
                    lineTo(0f, size.height - padBottom)
                    close()
                }

                // Draw the fill and the line
                drawPath(path = fillPath, brush = gradient)
                drawPath(path = linePath, color = lineColor, style = Stroke(width = 2.dp.toPx()))

                // Draw small data points
                history.forEachIndexed { i, lvl ->
                    val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                    val x = i * stepX
                    val y = padTop + (1 - lvl / 100f) * chartHeight
                    drawCircle(pointColor, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // X-Axis Labels (placed below the chart)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                // Add left padding to align with the start of the canvas
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-${history.size - 1}h", style = MaterialTheme.typography.labelSmall)
            Text("Now", style = MaterialTheme.typography.labelSmall)
        }
    }
}


/* ------------------ BAR CHART (REFACTORED FOR LAYOUT) ------------------ */
@Composable
private fun BarChart(history: List<Float>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("100%", style = MaterialTheme.typography.labelSmall)
                Text("75%", style = MaterialTheme.typography.labelSmall)
                Text("50%", style = MaterialTheme.typography.labelSmall)
                Text("25%", style = MaterialTheme.typography.labelSmall)
                Text("0%", style = MaterialTheme.typography.labelSmall)
            }

            // The actual Canvas for drawing the chart
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                val padTop = 18.dp.toPx()
                val padBottom = 18.dp.toPx()
                val chartHeight = size.height - padTop - padBottom

                // Draw subtle horizontal grid lines
                val gridLineCount = 4
                for (i in 0..gridLineCount) {
                    val y = padTop + i * (chartHeight / gridLineCount)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw bars
                val totalBarAndSpacingWidth = size.width / history.size
                val barWidth = totalBarAndSpacingWidth * 0.6f // 60% of available space
                val barSpacing = totalBarAndSpacingWidth * 0.4f
                history.forEachIndexed { i, lvl ->
                    val left = i * totalBarAndSpacingWidth + (barSpacing / 2)
                    val top = padTop + (1 - lvl / 100f) * chartHeight
                    val barHeight = chartHeight * (lvl / 100f)
                    drawRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(start = 24.dp), // Align with canvas
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-${history.size - 1}h", style = MaterialTheme.typography.labelSmall)
            Text("Now", style = MaterialTheme.typography.labelSmall)
        }
    }
}


/* ------------------ NEW HELPER for smooth line ------------------ */
/**
 * Creates a smooth cubic spline path through a list of points.
 */
private fun Path.cubicSplineTo(points: List<Offset>) {
    if (points.size < 2) return

    // Move to the first point
    moveTo(points.first().x, points.first().y)

    // Calculate control points and draw cubic bezier curves
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }

        // Tension factor, 0.5f is a good default
        val tension = 0.5f

        // Control point 1
        val cp1x = p1.x + (p2.x - p0.x) * tension / 3f
        val cp1y = p1.y + (p2.y - p0.y) * tension / 3f

        // Control point 2
        val cp2x = p2.x - (p3.x - p1.x) * tension / 3f
        val cp2y = p2.y - (p3.y - p1.y) * tension / 3f

        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
}
/* ------------------ HELPER DRAW ------------------ */
private fun DrawScope.drawGrid(pad: Float, gridColor: Color, w: Float, h: Float, verticalLines: Boolean = false, count: Int = 0) {
    // Horizontal grid lines (0%, 25%, 50%, 75%, 100%)
    for (i in 0..4) {
        val y = pad + i * (h - 2 * pad) / 4f
        drawLine(gridColor, Offset(pad, y), Offset(w - pad, y), strokeWidth = 1f)
    }
    // Optional vertical grid lines
    if (verticalLines && count > 1) {
        val chartW = w - 2 * pad
        val stepX = chartW / (count - 1).toFloat()
        for (i in 0 until count) {
            val x = pad + i * stepX
            drawLine(gridColor, Offset(x, pad), Offset(x, h - pad), strokeWidth = 0.5f)
        }
    }
}

private fun DrawScope.drawLabels(pad: Float, labelColor: Color, h: Float, textSizePx: Float) {
    val paint = android.graphics.Paint().apply {
        color = labelColor.toArgb(); textSize = textSizePx; isAntiAlias = true
    }
    val textHeightOffset = textSizePx / 3 // Approximate vertical centering
    drawContext.canvas.nativeCanvas.apply {
        drawText("100%", pad - (textSizePx * 2.5f), pad + textHeightOffset, paint)
        drawText("75%", pad - (textSizePx * 2.5f), pad + (h - 2 * pad) * 0.25f + textHeightOffset, paint)
        drawText("50%", pad - (textSizePx * 2.5f), pad + (h - 2 * pad) * 0.5f + textHeightOffset, paint)
        drawText("25%", pad - (textSizePx * 2.5f), pad + (h - 2 * pad) * 0.75f + textHeightOffset, paint)
        drawText("0%", pad - (textSizePx * 2.5f), h - pad + textHeightOffset, paint)
    }
}

private fun DrawScope.drawXAxisLabels(pad: Float, h: Float, w: Float, count: Int, labelColor: Color, textSizePx: Float) {
    val paint = android.graphics.Paint().apply {
        color = labelColor.toArgb(); textSize = textSizePx; isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val chartW = w - 2 * pad
    val stepX = if (count > 1) chartW / (count - 1).toFloat() else chartW
    val yPos = h - pad + textSizePx * 1.5f
    for (i in 0 until count) {
        val x = pad + i * stepX
        drawContext.canvas.nativeCanvas.drawText("${i + 1}", x, yPos, paint)
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