package id.xms.xtrakernelmanager.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.MainActivity
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class BatteryStatsService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    private val TAG = "BatteryStatsService"

    // Add variables to track charging state and smooth out readings
    private var lastChargingState = false
    private var lastCurrent = 0f
    private var stableReadingsCount = 0

    // Variables for screen time and drain tracking
    private var screenOnSince = 0L
    private var screenOnTime = 0L
    private var lastDrainTime = 0L
    private var lastDrainLevel = 0

    companion object {
        const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "battery_stats_channel"
        private const val UPDATE_INTERVAL = 1000L // 1 second
        private const val STABLE_READINGS_REQUIRED = 3 // Require 3 stable readings before changing state
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryStats(intent)
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (screenOnSince == 0L) {
                        screenOnSince = System.currentTimeMillis()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (screenOnSince > 0L) {
                        screenOnTime += System.currentTimeMillis() - screenOnSince
                        screenOnSince = 0L
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)
        startMonitoring()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Battery & System Stats",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows battery and system statistics"
            enableLights(false)
            enableVibration(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(
        batteryLevel: Int = 0,
        chargingCurrent: Float = 0f,
        voltage: Float = 0f,
        batteryTemp: Float = 0f,
        chargingType: String = "",
        drain: String = "N/A",
        screenOnTime: String = "N/A",
        deepSleep: String = "0%",
        awake: String = "0%",
        uptime: String = "0:00:00"
    ): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Format values with proper decimal places
        val formattedVoltage = if (voltage > 0) "%.2fV".format(voltage) else "N/A"
        val formattedCurrent = "%.0fmA".format(chargingCurrent / 1000)
        val formattedTemp = if (batteryTemp > 0) "%.1f°C".format(batteryTemp) else "N/A"
        val chargingInfo = if (chargingType.isNotEmpty()) " ($chargingType)" else ""

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("Battery & System Statistics")
            .bigText(
                """
                Battery: $batteryLevel% • $formattedTemp
                Current: $formattedCurrent$chargingInfo
                Drain: $drain • Screen: $screenOnTime
                Voltage: $formattedVoltage
                
                Deep Sleep: $deepSleep • Awake: $awake
                Uptime: $uptime
                """.trimIndent()
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery: $batteryLevel% • $formattedTemp")
            .setContentText("Current: $formattedCurrent • Drain: $drain")
            .setStyle(bigTextStyle)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Get system stats
                    val systemStats = getSystemStats()
                    // Update notification
                    updateNotification(systemStats)
                    delay(UPDATE_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring stats", e)
                    delay(UPDATE_INTERVAL * 2)
                }
            }
        }
    }

    private fun updateBatteryStats(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        // Check actual charging state to normalize current sign
        val isActuallyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0

        // Get raw current
        val rawCurrent = getCurrentNow()

        // Normalize current sign based on charging state
        val normalizedCurrent = when {
            rawCurrent == 0f -> 0f
            isActuallyCharging -> {
                // When charging, current should be positive
                if (rawCurrent < 0) -rawCurrent else rawCurrent
            }
            else -> {
                // When discharging, current should be negative
                if (rawCurrent > 0) -rawCurrent else rawCurrent
            }
        }

        // Simple smoothing
        val smoothedCurrent = if (lastCurrent == 0f) {
            lastCurrent = normalizedCurrent
            normalizedCurrent
        } else {
            val smoothed = (lastCurrent * 0.8f + normalizedCurrent * 0.2f)
            lastCurrent = smoothed
            smoothed
        }

        updateNotification(SystemStats(
            batteryLevel = level,
            chargingCurrent = smoothedCurrent,
            voltage = voltage,
            batteryTemp = getBatteryTemperature(),
            chargingType = getChargingTypeFromPlugged(plugged),
            drain = calculateDrain(level, plugged != 0),
            screenOnTime = getFormattedScreenOnTime(),
            deepSleep = getDeepSleepPercentage(),
            awake = getAwakePercentage(),
            uptime = getFormattedUptime()
        ))
    }

    private fun getChargingTypeFromPlugged(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> ""
        }
    }

    private fun getCurrentNow(): Float {
        try {
            // Try BatteryManager first (most reliable)
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentFromBatteryManager = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (currentFromBatteryManager != Int.MIN_VALUE) {
                Log.d(TAG, "Current from BatteryManager: $currentFromBatteryManager µA")
                return currentFromBatteryManager.toFloat()
            }

            // Try direct file reading with multiple possible locations
            val possiblePaths = listOf(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/battery/current_avg",
                "/sys/class/power_supply/bms/current_now",
                "/sys/class/power_supply/usb/current_now"
            )

            for (path in possiblePaths) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val currentStr = file.readText().trim()
                        val current = currentStr.toFloatOrNull()
                        if (current != null && current != 0f) {
                            Log.d(TAG, "Current from $path: $current µA")
                            return current
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to read from $path: ${e.message}")
                }
            }

            // Try using su command as fallback
            try {
                val process = Runtime.getRuntime().exec("su -c cat /sys/class/power_supply/battery/current_now")
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                val current = output.toFloatOrNull()
                if (current != null) {
                    Log.d(TAG, "Current from su: $current µA")
                    return current
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to read current via su: ${e.message}")
            }

            Log.w(TAG, "Could not read battery current from any source")
            return 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentNow()", e)
            return 0f
        }
    }

    private fun getSystemStats(): SystemStats {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Get voltage and charging status from battery intent
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)?.toFloat()?.div(1000f) ?: 0f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        // Check actual charging state to normalize current sign
        val isActuallyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0

        // Get raw current
        val rawCurrent = getCurrentNow()

        // Normalize current sign based on charging state (same logic as updateBatteryStats)
        val normalizedCurrent = when {
            rawCurrent == 0f -> 0f
            isActuallyCharging -> {
                // When charging, current should be positive
                if (rawCurrent < 0) -rawCurrent else rawCurrent
            }
            else -> {
                // When discharging, current should be negative
                if (rawCurrent > 0) -rawCurrent else rawCurrent
            }
        }

        // Simple smoothing
        val smoothedCurrent = if (lastCurrent == 0f) {
            lastCurrent = normalizedCurrent
            normalizedCurrent
        } else {
            val smoothed = (lastCurrent * 0.8f + normalizedCurrent * 0.2f)
            lastCurrent = smoothed
            smoothed
        }

        return SystemStats(
            batteryLevel = level,
            chargingCurrent = smoothedCurrent,
            voltage = voltage,
            batteryTemp = getBatteryTemperature(),
            chargingType = getChargingTypeFromPlugged(plugged),
            drain = calculateDrain(level, isActuallyCharging),
            screenOnTime = getFormattedScreenOnTime(),
            deepSleep = getDeepSleepPercentage(),
            awake = getAwakePercentage(),
            uptime = getFormattedUptime()
        )
    }

    private fun getBatteryTemperature(): Float {
        return try {
            // Get temperature from battery intent since BatteryManager doesn't have BATTERY_PROPERTY_TEMPERATURE
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            // Convert to Celsius, BatteryManager returns tenths of a degree
            temperature / 10f
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery temperature", e)
            0f
        }
    }

    private fun getChargingType(): String {
        return try {
            // Read from the same places as before, but now we also check the charging type
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

            when (plugged) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Not charging"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting charging type", e)
            "Unknown"
        }
    }

    private fun calculateDrain(currentLevel: Int, isCharging: Boolean): String {
        if (isCharging) {
            lastDrainLevel = currentLevel
            lastDrainTime = System.currentTimeMillis()
            return "Charging"
        }

        if (lastDrainLevel == 0) {
            lastDrainLevel = currentLevel
            lastDrainTime = System.currentTimeMillis()
            return "Calculating..."
        }

        val levelDrop = lastDrainLevel - currentLevel
        if (levelDrop <= 0) {
            return "0.0%/h"
        }

        val timeDiffMillis = System.currentTimeMillis() - lastDrainTime
        if (timeDiffMillis < 60000 * 5) { // Wait for at least 5 minutes of data
            return "Calculating..."
        }

        val drainPerHour = (levelDrop.toFloat() / timeDiffMillis.toFloat()) * 1000 * 60 * 60
        return "%.1f%%/h".format(drainPerHour)
    }

    private fun getFormattedScreenOnTime(): String {
        var currentScreenOnTime = screenOnTime
        if (screenOnSince > 0) {
            currentScreenOnTime += System.currentTimeMillis() - screenOnSince
        }
        val hours = TimeUnit.MILLISECONDS.toHours(currentScreenOnTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(currentScreenOnTime) % 60
        return "${hours}h ${minutes}m"
    }

    private fun getDeepSleepPercentage(): String {
        return try {
            // Use SystemClock methods which are more reliable than file access
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val awakeMillis = android.os.SystemClock.uptimeMillis()
            val deepSleepMillis = uptimeMillis - awakeMillis
            val percentage = (deepSleepMillis.toFloat() / uptimeMillis.toFloat()) * 100
            "%.1f%%".format(percentage)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating deep sleep percentage", e)
            "0.0%"
        }
    }

    private fun getAwakePercentage(): String {
        return try {
            // Use SystemClock methods which are more reliable
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val awakeMillis = android.os.SystemClock.uptimeMillis()
            val awakePercentage = (awakeMillis.toFloat() / uptimeMillis.toFloat()) * 100
            "%.1f%%".format(awakePercentage)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating awake percentage", e)
            "100.0%"
        }
    }

    private fun getFormattedUptime(): String {
        return try {
            // Use SystemClock.elapsedRealtime() which is more reliable
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val uptimeSeconds = uptimeMillis / 1000

            val days = uptimeSeconds / 86400
            val hours = (uptimeSeconds % 86400) / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60

            when {
                days > 0 -> "%dd %02dh %02dm %02ds".format(days, hours, minutes, seconds)
                hours > 0 -> "%02dh %02dm %02ds".format(hours, minutes, seconds)
                else -> "%02dm %02ds".format(minutes, seconds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting uptime", e)
            "0d 00h 00m 00s"
        }
    }

    private fun updateNotification(stats: SystemStats) {
        val notification = createNotification(
            batteryLevel = stats.batteryLevel,
            chargingCurrent = stats.chargingCurrent,
            voltage = stats.voltage,
            batteryTemp = stats.batteryTemp,
            chargingType = stats.chargingType,
            drain = stats.drain,
            screenOnTime = stats.screenOnTime,
            deepSleep = stats.deepSleep,
            awake = stats.awake,
            uptime = stats.uptime
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(screenStateReceiver)
            monitoringJob?.cancel()
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up service", e)
        }
        super.onDestroy()
    }

    data class SystemStats(
        val batteryLevel: Int,
        val chargingCurrent: Float,
        val voltage: Float,
        val batteryTemp: Float,
        val chargingType: String,
        val drain: String,
        val screenOnTime: String,
        val deepSleep: String,
        val awake: String,
        val uptime: String
    )
}
