package id.xms.xtrakernelmanager.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.xms.xtrakernelmanager.ui.GameControlSettingsActivity
import id.xms.xtrakernelmanager.ui.components.GlassIntensity
import id.xms.xtrakernelmanager.ui.components.SuperGlassCard
import id.xms.xtrakernelmanager.ui.viewmodel.DeveloperOptionsViewModel
import id.xms.xtrakernelmanager.viewmodel.MiscViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    navController: NavController,
    miscViewModel: MiscViewModel = hiltViewModel(),
    devOptionsViewModel: DeveloperOptionsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Use remember to cache collectAsState to avoid recomposition
    val batteryStatsEnabled by remember { miscViewModel.batteryStatsEnabled }.collectAsState()
    val batteryNotificationEnabled by remember { miscViewModel.batteryNotificationEnabled }.collectAsState()
    val showBatteryTempInStatusBar by remember { miscViewModel.showBatteryTempInStatusBar }.collectAsState()
    val gameControlEnabled by remember { miscViewModel.fpsMonitorEnabled }.collectAsState()
    val hideDeveloperOptionsEnabled by remember { devOptionsViewModel.hideDeveloperOptionsEnabled }

    // State for controlling card expansion (like in Tuning)
    var batteryCardExpanded by remember { mutableStateOf(false) }
    var gameControlCardExpanded by remember { mutableStateOf(false) }
    var systemTweaksCardExpanded by remember { mutableStateOf(false) }
    var devOptionsCardExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Misc Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Battery Stats & Notification Card with dropdown
        BatteryStatsCard(
            batteryStatsEnabled = batteryStatsEnabled,
            batteryNotificationEnabled = batteryNotificationEnabled,
            showBatteryTempInStatusBar = showBatteryTempInStatusBar,
            expanded = batteryCardExpanded,
            onExpandedChange = { batteryCardExpanded = it },
            onToggleBatteryStats = { enabled ->
                miscViewModel.toggleBatteryStats(enabled)
            },
            onToggleShowBatteryTempInStatusBar = { enabled ->
                miscViewModel.toggleShowBatteryTempInStatusBar(enabled)
            }
        )

        // Game Control Card with dropdown
        GameControlCard(
            gameControlEnabled = gameControlEnabled,
            expanded = gameControlCardExpanded,
            onExpandedChange = { gameControlCardExpanded = it },
            onToggleGameControl = { enabled ->
                if (enabled && !Settings.canDrawOverlays(context)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    miscViewModel.toggleGameControl(enabled)
                }
            }
        )

        // System Tweaks Card with dropdown
        SystemTweaksCard(
            expanded = systemTweaksCardExpanded,
            onExpandedChange = { systemTweaksCardExpanded = it }
        )

        // Developer Options Card with dropdown - lazy load only when expanded
        DeveloperOptionsCard(
            navController = navController,
            hideDeveloperOptionsEnabled = hideDeveloperOptionsEnabled,
            expanded = devOptionsCardExpanded,
            onExpandedChange = { devOptionsCardExpanded = it },
            onToggleHideDeveloperOptions = { devOptionsViewModel.setHideDeveloperOptions(it) }
        )
    }
}

@Composable
fun BatteryStatsCard(
    batteryStatsEnabled: Boolean,
    batteryNotificationEnabled: Boolean,
    showBatteryTempInStatusBar: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleBatteryStats: (Boolean) -> Unit,
    onToggleShowBatteryTempInStatusBar: (Boolean) -> Unit
) {
    SuperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Battery & System Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Expandable header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Details" else "Show Details",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                // Battery Stats Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Stats Service",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Monitor battery usage, charging stats, and system metrics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = batteryStatsEnabled,
                        onCheckedChange = onToggleBatteryStats
                    )
                }

                // Battery Notification Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persistent Notification",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Show detailed battery info in notification panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = batteryNotificationEnabled,
                        onCheckedChange = onToggleBatteryStats,
                        enabled = false // This is controlled by the main battery stats toggle
                    )
                }

                // Show Battery Temp in Status Bar Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Battery Temperature in Status Bar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Display battery temperature next to percentage in the status bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showBatteryTempInStatusBar,
                        onCheckedChange = onToggleShowBatteryTempInStatusBar,
                        enabled = batteryStatsEnabled
                    )
                }

                if (batteryStatsEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryStd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Battery Service Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "Monitoring battery level, charging current, voltage, temperature, screen time, and deep sleep statistics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (!batteryStatsEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Features Available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("• Real-time battery level & charging status", style = MaterialTheme.typography.bodySmall)
                                Text("• Charging current & voltage monitoring", style = MaterialTheme.typography.bodySmall)
                                Text("• Battery temperature tracking", style = MaterialTheme.typography.bodySmall)
                                Text("• Screen on/off time statistics", style = MaterialTheme.typography.bodySmall)
                                Text("• Deep sleep & awake time monitoring", style = MaterialTheme.typography.bodySmall)
                                Text("• System uptime tracking", style = MaterialTheme.typography.bodySmall)
                                Text("• Battery drain rate calculation", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameControlCard(
    gameControlEnabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleGameControl: (Boolean) -> Unit
) {
    val context = LocalContext.current

    SuperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Games,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Game Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            // Game Control description
            Text(
                text = "Enhance your gaming experience with advanced performance monitoring and controls",
                style = MaterialTheme.typography.bodyMedium
            )

            // Feature list
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BulletPoint("Real-time FPS monitoring with graph visualization")
                BulletPoint("CPU & GPU load and governor information")
                BulletPoint("Performance profiles (Default, Battery, Performance)")
                BulletPoint("Quick access to Do Not Disturb mode")
                BulletPoint("Clear background apps with one tap")
                BulletPoint("Configure per-app settings for automatic activation")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Main Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Game Control",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Master switch for the enhanced Game Control feature",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = gameControlEnabled,
                    onCheckedChange = onToggleGameControl
                )
            }

            // Configure Apps Button
            Button(
                onClick = {
                    val intent = Intent(context, GameControlSettingsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Game Apps")
            }

            // Status indicator when enabled
            if (gameControlEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Games,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Game Control Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Game Control overlay will automatically appear when you launch configured games. Configure specific apps to customize their gaming experience.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Overlay Permission Warning (if enabled but no permission)
            if (gameControlEnabled && !Settings.canDrawOverlays(context)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Overlay Permission Required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "Game Control requires overlay permission to display performance monitoring and controls on top of games. Please grant the permission to use this feature.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("GRANT PERMISSION")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SystemTweaksCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isClearing by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var cacheClearedMB by remember { mutableStateOf(0L) }
    var appsCleared by remember { mutableStateOf(0) }
    var useRoot by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var liveLog by remember { mutableStateOf(listOf<String>()) }
    var progress by remember { mutableStateOf(0f) }
    var isCanceled by remember { mutableStateOf(false) }
    var minCacheOption by remember { mutableStateOf("10MB") }
    val cacheOptions = listOf("1MB", "10MB", "50MB", "100MB", "500MB", "Unlimited")
    val minCacheMB = when (minCacheOption) {
        "1MB" -> 1
        "10MB" -> 10
        "50MB" -> 50
        "100MB" -> 100
        "500MB" -> 500
        else -> 0 // Unlimited
    }
    val scope = rememberCoroutineScope()

    SuperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Tweaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Toggle for root/non-root
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Use Root Method", modifier = Modifier.weight(1f))
                Switch(checked = useRoot, onCheckedChange = { useRoot = it })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Min cache to clear:", modifier = Modifier.weight(1f))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded = true }) {
                        Text(minCacheOption)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        cacheOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                minCacheOption = option
                                expanded = false
                            })
                        }
                    }
                }
            }
            // Content only visible when card is expanded
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear All Cache")
                        Text(
                            text = if (useRoot) "Root: Remove cache for all apps (requires root access)" else "Non-root: Remove cache for accessible apps only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            isClearing = true
                            cacheClearedMB = 0L
                            appsCleared = 0
                            errorMsg = ""
                            liveLog = listOf("Starting cache clear...")
                            progress = 0f
                            isCanceled = false
                            scope.launch {
                                if (useRoot) {
                                    try {
                                        // Check root access first
                                        val suTest = Runtime.getRuntime().exec("su -c echo rooted")
                                        val suTestOut = suTest.inputStream.bufferedReader().readText().trim()
                                        val suTestErr = suTest.errorStream.bufferedReader().readText().trim()
                                        val suTestCode = suTest.waitFor()
                                        if (suTestCode != 0 || !suTestOut.contains("rooted")) {
                                            errorMsg = "Root access not available.\nError: $suTestErr"
                                        } else {
                                            // List all cache folders
                                            val listCmd = "find /data/data -type d -name cache"
                                            val listProc = Runtime.getRuntime().exec(arrayOf("su", "-c", listCmd))
                                            val cacheFolders = listProc.inputStream.bufferedReader().readLines()
                                            var totalCacheBefore = 0L
                                            var totalCacheAfter = 0L
                                            var clearedAppsCount = 0
                                            val logList = mutableListOf<String>()
                                            val totalFolders = cacheFolders.size
                                            var processedFolders = 0
                                            for (folder in cacheFolders) {
                                                if (isCanceled) break
                                                // Get size before
                                                val sizeCmd = "du -sb $folder"
                                                val sizeProc = Runtime.getRuntime().exec(arrayOf("su", "-c", sizeCmd))
                                                val sizeOut = sizeProc.inputStream.bufferedReader().readText().trim()
                                                val sizeBefore = sizeOut.split("\t").firstOrNull()?.toLongOrNull() ?: 0L
                                                val sizeBeforeMB = sizeBefore / (1024 * 1024)
                                                if (sizeBeforeMB < minCacheMB) {
                                                    processedFolders++
                                                    progress = processedFolders / totalFolders.toFloat()
                                                    continue
                                                }
                                                totalCacheBefore += sizeBefore
                                                // Clear cache
                                                val rmCmd = "rm -rf $folder/*"
                                                val rmProc = Runtime.getRuntime().exec(arrayOf("su", "-c", rmCmd))
                                                val rmExit = rmProc.waitFor()
                                                // Get size after
                                                val sizeProc2 = Runtime.getRuntime().exec(arrayOf("su", "-c", sizeCmd))
                                                val sizeOut2 = sizeProc2.inputStream.bufferedReader().readText().trim()
                                                val sizeAfter = sizeOut2.split("\t").firstOrNull()?.toLongOrNull() ?: 0L
                                                totalCacheAfter += sizeAfter
                                                if (sizeBefore > 0) clearedAppsCount++
                                                logList.add("${folder}: ${sizeBeforeMB}MB -> ${sizeAfter / (1024 * 1024)}MB")
                                                liveLog = logList.toList() // update UI
                                                processedFolders++
                                                progress = processedFolders / totalFolders.toFloat()
                                            }
                                            cacheClearedMB = (totalCacheBefore - totalCacheAfter) / (1024 * 1024)
                                            appsCleared = clearedAppsCount
                                            liveLog = logList + listOf("Done! Total cleared: $cacheClearedMB MB from $appsCleared apps.")
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = "Root method error: ${e.message}".take(200)
                                    }
                                } else {
                                    // Non-root method
                                    val pm = context.packageManager
                                    val packages = pm.getInstalledPackages(0)
                                    val totalApps = packages.size
                                    var processedApps = 0
                                    var totalCache = 0L
                                    var clearedApps = 0
                                    val logList = mutableListOf<String>()
                                    for (pkg in packages) {
                                        if (isCanceled) break
                                        try {
                                            val cacheDir = context.createPackageContext(pkg.packageName, 0).cacheDir
                                            val cacheSize = cacheDir?.listFiles()?.sumOf { it.length() } ?: 0L
                                            val cacheSizeMB = cacheSize / (1024 * 1024)
                                            if (cacheSizeMB < minCacheMB) {
                                                processedApps++
                                                progress = processedApps / totalApps.toFloat()
                                                continue
                                            }
                                            if (cacheSize > 0) {
                                                cacheDir?.listFiles()?.forEach { it.delete() }
                                                totalCache += cacheSize
                                                clearedApps++
                                                logList.add("${pkg.packageName}: ${cacheSizeMB}MB cleared")
                                                liveLog = logList.toList()
                                            }
                                            processedApps++
                                            progress = processedApps / totalApps.toFloat()
                                        } catch (_: Exception) {
                                            processedApps++
                                            progress = processedApps / totalApps.toFloat()
                                        }
                                    }
                                    cacheClearedMB = totalCache / (1024 * 1024)
                                    appsCleared = clearedApps
                                    liveLog = logList + listOf("Done! Total cleared: $cacheClearedMB MB from $appsCleared apps.")
                                }
                                isClearing = false
                                showResultDialog = true
                            }
                        },
                        enabled = !isClearing
                    ) {
                        Text(if (isClearing) "Clearing..." else "Clear")
                    }
                }
            }
        }
    }
    if (isClearing) {
        AlertDialog(
            onDismissRequest = { isCanceled = true },
            title = { Text("Clearing Cache" + if (useRoot) " (Root)" else " (Non-root)") },
            text = {
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    liveLog.forEach { log ->
                        Text(log, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { isCanceled = true }) { Text("Cancel") }
            }
        )
    }
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("Cache Cleared" + if (useRoot) " (Root)" else " (Non-root)") },
            text = {
                if (errorMsg.isNotEmpty()) {
                    Text("Error: $errorMsg")
                } else {
                    Text("Successfully cleared $cacheClearedMB MB of cache from $appsCleared apps.")
                }
            },
            confirmButton = {
                Button(onClick = { showResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

// Helper functions for root estimation (simulate, not exact)
private fun getTotalCacheSizeRoot(): Long {
    // In real root implementation, you would run a shell command to sum all /data/data/*/cache sizes
    // Here, just return 0 for simulation
    return 0L
}
private fun getAppCountRoot(): Int {
    // In real root implementation, you would count all /data/data/*/cache folders
    // Here, just return 0 for simulation
    return 0
}

@Composable
fun DeveloperOptionsCard(
    navController: NavController,
    hideDeveloperOptionsEnabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleHideDeveloperOptions: (Boolean) -> Unit
) {
    SuperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Developer Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Expandable header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Details" else "Show Details",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Content only visible when card is expanded
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Developer Options")
                        Text(
                            text = "Hide developer options for specific apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hideDeveloperOptionsEnabled,
                        onCheckedChange = onToggleHideDeveloperOptions
                    )
                }

                if (hideDeveloperOptionsEnabled) {
                    Button(
                        onClick = { navController.navigate("app_selection") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Apps")
                    }
                }
            }
        }
    }
}
