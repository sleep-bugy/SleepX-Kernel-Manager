package id.xms.xtrakernelmanager.service

import android.app.*
import android.appwidget.AppWidgetManager
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
import id.xms.xtrakernelmanager.utils.PreferenceManager
import kotlinx.coroutines.*
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BatteryStatsService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    private val TAG = "BatteryStatsService"

    @Inject
    lateinit var preferenceManager: PreferenceManager

    // Add variables to track charging state and smooth out readings
    private var lastChargingState = false
    private var lastCurrent = 0f
    private var stableReadingsCount = 0

    // Variables for screen time and drain tracking
    private var screenOnSince = 0L
    private var screenOnTime = 0L
    private var screenOffSince = 0L
    private var screenOffTime = 0L
    private var lastDrainTime = 0L
    private var lastDrainLevel = 0

    // Cache for accessible battery paths to avoid repeated SELinux denials
    private val accessibleBatteryPaths = mutableSetOf<String>()
    private val inaccessibleBatteryPaths = mutableSetOf<String>()
    private var batteryPathsChecked = false

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
                    // Update screen off time if screen was off
                    if (screenOffSince > 0L) {
                        screenOffTime += System.currentTimeMillis() - screenOffSince
                        screenOffSince = 0L
                    }
                    // Start tracking screen on time
                    if (screenOnSince == 0L) {
                        screenOnSince = System.currentTimeMillis()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Update screen on time if screen was on
                    if (screenOnSince > 0L) {
                        screenOnTime += System.currentTimeMillis() - screenOnSince
                        screenOnSince = 0L
                    }
                    // Start tracking screen off time
                    if (screenOffSince == 0L) {
                        screenOffSince = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BatteryStatsService onCreate")

        // Mark service as enabled in preferences for auto-start on next boot
        preferenceManager.setBatteryStatsEnabled(true)

        // Update widgets to reflect the new state
        updateWidgets()

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BatteryStatsService onStartCommand")

        // Mark service as enabled in preferences
        preferenceManager.setBatteryStatsEnabled(true)

        // Return START_STICKY to automatically restart the service if killed
        return START_STICKY
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
        chargingStatus: String = "Unknown",
        batteryHealth: String = "Unknown",
        healthPercentage: Int = 0,
        cycleCount: Int = 0,
        batteryCapacity: Int = 0,
        currentCapacity: Int = 0,
        batteryTechnology: String = "Unknown",
        drain: String = "N/A",
        screenOnTime: String = "N/A",
        screenOffTime: String = "N/A",
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
        val chargingInfo = if (chargingType.isNotEmpty() && chargingType != "Unknown") " ($chargingType)" else ""

        // Format capacity information
        val capacityInfo = if (batteryCapacity > 0) {
            if (currentCapacity > 0) {
                "$currentCapacity/$batteryCapacity mAh"
            } else {
                "$batteryCapacity mAh"
            }
        } else "N/A"

        // Format health information
        val healthInfo = if (healthPercentage > 0) {
            "$batteryHealth ($healthPercentage%)"
        } else {
            batteryHealth
        }

        // Format cycle information
        val cycleInfo = if (cycleCount > 0) "$cycleCount cycles" else "N/A"

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("Battery & System Statistics")
            .bigText(
                """
                🔋 Battery: $batteryLevel% • $formattedTemp • $chargingStatus
                ⚡ Power: $formattedCurrent$chargingInfo • Drain: $drain
                🔬 Health: $healthInfo • Cycles: $cycleInfo
                📊 Capacity: $capacityInfo • Tech: $batteryTechnology
                📱 Screen: On $screenOnTime • Off $screenOffTime
                ⚙️ Voltage: $formattedVoltage • Uptime: $uptime
                
                💤 Deep Sleep: $deepSleep • Awake: $awake
                """.trimIndent()
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔋 $batteryLevel% • $formattedTemp • $chargingStatus")
            .setContentText("⚡ $formattedCurrent$chargingInfo • 🔬 $healthInfo")
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
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

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

        // Get enhanced battery information
        val chargingStatus = getChargingStatusString(status, plugged)
        val healthString = getBatteryHealthString(health)
        val healthPercentage = getBatteryHealthPercentage()
        val cycleCount = getBatteryCycleCount()
        val capacity = getBatteryCapacity()
        val currentCapacity = getCurrentCapacity()

        updateNotification(SystemStats(
            batteryLevel = level,
            chargingCurrent = smoothedCurrent,
            voltage = voltage,
            batteryTemp = getBatteryTemperature(),
            chargingType = getChargingTypeFromPlugged(plugged),
            chargingStatus = chargingStatus,
            batteryHealth = healthString,
            healthPercentage = healthPercentage,
            cycleCount = cycleCount,
            batteryCapacity = capacity,
            currentCapacity = currentCapacity,
            batteryTechnology = technology,
            drain = calculateDrain(level, plugged != 0),
            screenOnTime = getFormattedScreenOnTime(),
            screenOffTime = getFormattedScreenOffTime(),
            deepSleep = getDeepSleepPercentage(),
            awake = getAwakePercentage(),
            uptime = getFormattedUptime()
        ))
    }

    private fun getChargingStatusString(status: Int, plugged: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> {
                if (plugged != 0) "Not Charging" else "Discharging"
            }
            else -> "Unknown"
        }
    }

    private fun getBatteryHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    private fun checkBatteryPathAccess(path: String): Boolean {
        // Return cached result if already checked
        if (batteryPathsChecked) {
            return accessibleBatteryPaths.contains(path)
        }

        try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                // Try to actually read the file to verify access
                file.readText().trim()
                accessibleBatteryPaths.add(path)
                return true
            }
        } catch (e: SecurityException) {
            // SELinux denial - cache as inaccessible to avoid future attempts
            inaccessibleBatteryPaths.add(path)
            Log.d(TAG, "Path $path is not accessible due to SELinux policy")
        } catch (e: Exception) {
            inaccessibleBatteryPaths.add(path)
            Log.d(TAG, "Path $path is not accessible: ${e.message}")
        }

        return false
    }

    private fun getBatteryHealthPercentage(): Int {
        try {
            // Use only BatteryManager API first to avoid SELinux issues
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            // Try to get capacity info from BatteryManager
            val designCapacity = getBatteryCapacityViaBatteryManager()
            val currentCapacity = getCurrentCapacityViaBatteryManager()

            if (designCapacity > 0 && currentCapacity > 0) {
                val healthPercentage = ((currentCapacity.toFloat() / designCapacity) * 100).toInt()
                Log.d(TAG, "Battery Health via BatteryManager: $healthPercentage%")
                return healthPercentage
            }

            // Only try file system access if BatteryManager fails and we haven't marked paths as inaccessible
            if (!batteryPathsChecked) {
                val healthPaths = listOf(
                    "/sys/class/power_supply/battery/health",
                    "/sys/class/power_supply/battery/capacity_level",
                    "/sys/class/power_supply/bms/battery_health"
                )

                for (path in healthPaths) {
                    if (checkBatteryPathAccess(path)) {
                        try {
                            val file = File(path)
                            val content = file.readText().trim()
                            val percentage = content.toIntOrNull()
                            if (percentage != null && percentage in 0..100) {
                                return percentage
                            }
                        } catch (e: Exception) {
                            // Continue to next path
                        }
                    }
                }
                batteryPathsChecked = true
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery health percentage: ${e.message}")
        }
        return 100 // Default to 100% if unknown
    }

    private fun getBatteryCapacityViaBatteryManager(): Int {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            // Try to get design capacity via BatteryManager (Android 6.0+)
            val designCapacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (designCapacity != Int.MIN_VALUE && designCapacity > 0) {
                // Convert from nWh to mAh (approximate)
                return (designCapacity / 3700).coerceAtLeast(1000) // Assume ~3.7V average
            }
        } catch (e: Exception) {
            Log.d(TAG, "BatteryManager design capacity not available: ${e.message}")
        }
        return 0
    }

    private fun getCurrentCapacityViaBatteryManager(): Int {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentEnergy = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (currentEnergy != Int.MIN_VALUE && currentEnergy > 0) {
                // Convert from nWh to mAh (approximate)
                return (currentEnergy / 3700).coerceAtLeast(500)
            }
        } catch (e: Exception) {
            Log.d(TAG, "BatteryManager current capacity not available: ${e.message}")
        }
        return 0
    }

    private fun getBatteryCycleCount(): Int {
        try {
            val cyclePaths = listOf(
                "/sys/class/power_supply/battery/cycle_count",
                "/sys/class/power_supply/bms/cycle_count",
                "/proc/driver/battery_cycle"
            )

            for (path in cyclePaths) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val content = file.readText().trim()
                        val cycles = content.toIntOrNull()
                        if (cycles != null && cycles >= 0) {
                            Log.d(TAG, "Found battery cycles from $path: $cycles")
                            return cycles
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next path
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery cycle count: ${e.message}")
        }
        return 0
    }

    private fun getBatteryCapacity(): Int {
        try {
            // Try reading from system files
            val capacityPaths = listOf(
                "/sys/class/power_supply/battery/charge_full_design",
                "/sys/class/power_supply/battery/energy_full_design",
                "/sys/class/power_supply/bms/charge_full_design"
            )

            for (path in capacityPaths) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val content = file.readText().trim()
                        val cap = content.toLongOrNull()
                        if (cap != null && cap > 0) {
                            val capacityMah = when {
                                cap > 10000000 -> (cap / 1000).toInt() // µAh to mAh
                                cap > 10000 -> (cap / 1000).toInt() // µAh to mAh
                                else -> cap.toInt() // Already in mAh
                            }
                            Log.d(TAG, "Found design capacity from $path: $capacityMah mAh")
                            return capacityMah
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next path
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery capacity: ${e.message}")
        }
        return 0
    }

    private fun getCurrentCapacity(): Int {
        try {
            val capacityPaths = listOf(
                "/sys/class/power_supply/battery/charge_full",
                "/sys/class/power_supply/battery/energy_full",
                "/sys/class/power_supply/bms/charge_full"
            )

            for (path in capacityPaths) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val content = file.readText().trim()
                        val cap = content.toLongOrNull()
                        if (cap != null && cap > 0) {
                            val capacityMah = when {
                                cap > 10000000 -> (cap / 1000).toInt() // µAh to mAh
                                cap > 10000 -> (cap / 1000).toInt() // µAh to mAh
                                else -> cap.toInt() // Already in mAh
                            }
                            Log.d(TAG, "Found current capacity from $path: $capacityMah mAh")
                            return capacityMah
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next path
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting current capacity: ${e.message}")
        }
        return 0
    }

    private fun getSystemStats(): SystemStats {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Get voltage and charging status from battery intent
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)?.toFloat()?.div(1000f) ?: 0f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

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

        // Get enhanced battery information
        val chargingStatus = getChargingStatusString(status, plugged)
        val healthString = getBatteryHealthString(health)
        val healthPercentage = getBatteryHealthPercentage()
        val cycleCount = getBatteryCycleCount()
        val capacity = getBatteryCapacity()
        val currentCapacity = getCurrentCapacity()

        return SystemStats(
            batteryLevel = level,
            chargingCurrent = smoothedCurrent,
            voltage = voltage,
            batteryTemp = getBatteryTemperature(),
            chargingType = getChargingTypeFromPlugged(plugged),
            chargingStatus = chargingStatus,
            batteryHealth = healthString,
            healthPercentage = healthPercentage,
            cycleCount = cycleCount,
            batteryCapacity = capacity,
            currentCapacity = currentCapacity,
            batteryTechnology = technology,
            drain = calculateDrain(level, isActuallyCharging),
            screenOnTime = getFormattedScreenOnTime(),
            screenOffTime = getFormattedScreenOffTime(),
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
        return formatTimeWithSeconds(currentScreenOnTime)
    }

    private fun getFormattedScreenOffTime(): String {
        var currentScreenOffTime = screenOffTime
        if (screenOffSince > 0) {
            currentScreenOffTime += System.currentTimeMillis() - screenOffSince
        }
        return formatTimeWithSeconds(currentScreenOffTime)
    }

    private fun formatTimeWithSeconds(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
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
            chargingStatus = stats.chargingStatus,
            batteryHealth = stats.batteryHealth,
            healthPercentage = stats.healthPercentage,
            cycleCount = stats.cycleCount,
            batteryCapacity = stats.batteryCapacity,
            currentCapacity = stats.currentCapacity,
            batteryTechnology = stats.batteryTechnology,
            drain = stats.drain,
            screenOnTime = stats.screenOnTime,
            screenOffTime = stats.screenOffTime,
            deepSleep = stats.deepSleep,
            awake = stats.awake,
            uptime = stats.uptime
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getCurrentNow(): Float {
        try {
            // Try BatteryManager first (most reliable and doesn't trigger SELinux warnings)
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentFromBatteryManager = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (currentFromBatteryManager != Int.MIN_VALUE) {
                return currentFromBatteryManager.toFloat()
            }

            // Only try file system access if we haven't already determined the paths are inaccessible
            if (!batteryPathsChecked) {
                val possiblePaths = listOf(
                    "/sys/class/power_supply/battery/current_now",
                    "/sys/class/power_supply/battery/current_avg",
                    "/sys/class/power_supply/bms/current_now",
                    "/sys/class/power_supply/usb/current_now"
                )

                for (path in possiblePaths) {
                    if (checkBatteryPathAccess(path)) {
                        try {
                            val file = File(path)
                            val currentStr = file.readText().trim()
                            val current = currentStr.toFloatOrNull()
                            if (current != null && current != 0f) {
                                return current
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Failed to read from $path: ${e.message}")
                        }
                    }
                }
                batteryPathsChecked = true
            }

            // Use cached accessible paths only
            for (path in accessibleBatteryPaths) {
                if (path.contains("current")) {
                    try {
                        val file = File(path)
                        val currentStr = file.readText().trim()
                        val current = currentStr.toFloatOrNull()
                        if (current != null) {
                            return current
                        }
                    } catch (e: Exception) {
                        // Path might have become inaccessible, remove from cache
                        accessibleBatteryPaths.remove(path)
                        inaccessibleBatteryPaths.add(path)
                    }
                }
            }

            return 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentNow()", e)
            return 0f
        }
    }

    private fun getChargingTypeFromPlugged(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> ""
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "BatteryStatsService onDestroy")
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(screenStateReceiver)
            monitoringJob?.cancel()
            serviceScope.cancel()

            // Don't disable the preference here - keep it enabled for auto-start on boot
            // Only disable if explicitly stopped by user through UI
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up service", e)
        }
        super.onDestroy()

        // Restart the service if it was killed unexpectedly and auto-start is enabled
        if (preferenceManager.getBatteryNotificationAutoStart() && preferenceManager.getBatteryStatsEnabled()) {
            Log.d(TAG, "Attempting to restart BatteryStatsService")
            val restartIntent = Intent(applicationContext, BatteryStatsService::class.java)
            try {
                startForegroundService(restartIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service: ${e.message}")
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "BatteryStatsService onTaskRemoved")
        super.onTaskRemoved(rootIntent)

        // Keep service running even when app is removed from recent apps
        if (preferenceManager.getServiceAutoStart()) {
            val restartIntent = Intent(applicationContext, BatteryStatsService::class.java)
            startForegroundService(restartIntent)
        }
    }

    private fun updateWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = android.content.ComponentName(this, id.xms.xtrakernelmanager.widget.BatteryNotificationWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "Updating ${appWidgetIds.size} widgets")
                // Send broadcast to update widgets
                val updateIntent = Intent(this, id.xms.xtrakernelmanager.widget.BatteryNotificationWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
        }
    }

    data class SystemStats(
        val batteryLevel: Int,
        val chargingCurrent: Float,
        val voltage: Float,
        val batteryTemp: Float,
        val chargingType: String,
        val chargingStatus: String = "Unknown",
        val batteryHealth: String = "Unknown",
        val healthPercentage: Int = 0,
        val cycleCount: Int = 0,
        val batteryCapacity: Int = 0,
        val currentCapacity: Int = 0,
        val batteryTechnology: String = "Unknown",
        val drain: String,
        val screenOnTime: String,
        val screenOffTime: String,
        val deepSleep: String,
        val awake: String,
        val uptime: String
    )
}
