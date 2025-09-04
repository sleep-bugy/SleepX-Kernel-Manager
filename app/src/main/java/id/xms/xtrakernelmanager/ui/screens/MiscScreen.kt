package id.xms.xtrakernelmanager.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.service.BatteryStatsService
import id.xms.xtrakernelmanager.ui.viewmodel.MiscViewModel

@Composable
fun MiscScreen(
    viewModel: MiscViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(false) }

    // Check service status when composable is first created
    LaunchedEffect(Unit) {
        isServiceRunning = isServiceRunning(context, BatteryStatsService::class.java)
    }

    // Periodically check service status to keep UI in sync
    LaunchedEffect(isServiceRunning) {
        kotlinx.coroutines.delay(2000L) // Check every 2 seconds
        val actualServiceStatus = isServiceRunning(context, BatteryStatsService::class.java)
        if (actualServiceStatus != isServiceRunning) {
            isServiceRunning = actualServiceStatus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Battery & System Stats",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Shows real-time battery statistics and system information in notification",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isServiceRunning) "Running" else "Stopped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { checked ->
                            val intent = Intent(context, BatteryStatsService::class.java)
                            if (checked) {
                                try {
                                    context.startForegroundService(intent)
                                    isServiceRunning = true
                                } catch (e: Exception) {
                                    // If failed to start, keep switch off
                                    isServiceRunning = false
                                }
                            } else {
                                context.stopService(intent)
                                isServiceRunning = false
                            }
                        }
                    )
                }
            }
        }
    }
}

// Helper function to check if service is actually running
private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}
