package id.xms.xtrakernelmanager.service

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.MainActivity
import id.xms.xtrakernelmanager.utils.PreferenceManager
import kotlinx.coroutines.*
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class BatteryStatsService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    private val TAG = "BatteryStatsService"

    @Inject
    lateinit var preferenceManager: PreferenceManager

    // Add variables to track charging state and smooth out readings
    private var lastCurrent = 0f

    // Variables for screen time and drain tracking
    private var screenOnSince = 0L
    private var screenOnTime = 0L
    private var screenOffSince = 0L
    private var screenOffTime = 0L
    private var lastDrainTime = 0L
    private var lastDrainLevel = 0

    // Variabel untuk Idle Drain
    private var idleDrainStartTime = 0L
    private var idleDrainStartLevel = 0

    // Cache for accessible battery paths to avoid repeated SELinux denials
    private val accessibleBatteryPaths = mutableSetOf<String>()
    private val inaccessibleBatteryPaths = mutableSetOf<String>()
    private var batteryPathsChecked = false

    companion object {
        const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "battery_stats_channel"
        private const val UPDATE_INTERVAL = 1000L // 1 second
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
                    if (screenOffSince > 0L) {
                        screenOffTime += System.currentTimeMillis() - screenOffSince
                        screenOffSince = 0L
                    }
                    if (screenOnSince == 0L) {
                        screenOnSince = System.currentTimeMillis()
                    }
                    idleDrainStartTime = 0L
                    idleDrainStartLevel = 0
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (screenOnSince > 0L) {
                        screenOnTime += System.currentTimeMillis() - screenOnSince
                        screenOnSince = 0L
                    }
                    if (screenOffSince == 0L) {
                        screenOffSince = System.currentTimeMillis()
                    }
                    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    idleDrainStartLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    idleDrainStartTime = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BatteryStatsService onCreate")
        preferenceManager.setBatteryStatsEnabled(true)
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
        preferenceManager.setBatteryStatsEnabled(true)
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

    // Function to create a bitmap icon with battery percentage
    private fun createBatteryPercentIcon(percent: Int): IconCompat {
        val size = 48 // px, recommended for notification icons
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
        paint.color = Color.parseColor("#2196F3")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.6f
        val percentText = percent.toString()
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(percentText, size / 2f, y, paint)
        return IconCompat.createWithBitmap(bitmap)
    }
    private fun createNotification(
        batteryLevel: Int = 0,
        chargingCurrent: Float = 0f,
        voltage: Float = 0f,
        wattage: Float = 0f, // BARU
        batteryTemp: Float = 0f,
        chargingType: String = "",
        chargingStatus: String = "Unknown",
        batteryTechnology: String = "Unknown",
        drain: String = "N/A",
        idleDrain: String = "N/A",
        screenOnTime: String = "00:00:00",
        screenOffTime: String = "00:00:00",
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

        // Format values
        val formattedVoltage = if (voltage > 0) "%.2fV".format(voltage) else "N/A"
        val formattedCurrent = "%.0fmA".format(chargingCurrent / 1000)
        val formattedTemp = if (batteryTemp > 0) "%.1f°C".format(batteryTemp) else "N/A"
        val formattedWattage = "%.2fW".format(wattage) // BARU
        val chargingInfo = if (chargingType.isNotEmpty() && chargingType != "Unknown") " ($chargingType)" else ""

        // MODIFIKASI: Tambahkan Watt ke notifikasi
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("Battery & System Statistics")
            .bigText(
                """
                Battery : $batteryLevel% • $formattedTemp • $chargingStatus
                Power : $formattedCurrent / $formattedWattage$chargingInfo
                Drain : Active: $drain • Idle: $idleDrain
                Screen On: $screenOnTime • Screen Off: $screenOffTime
                Voltage : $formattedVoltage • Uptime: $uptime
                Deep Sleep : $deepSleep • Awake: $awake
                """.trimIndent()
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔋 $batteryLevel% • $formattedTemp • $chargingStatus")
            .setContentText("⚡ $formattedCurrent / $formattedWattage • Drain: $drain")
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
                    val systemStats = getSystemStats()
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

        val isActuallyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0
        val rawCurrent = getCurrentNow()

        val normalizedCurrent = when {
            rawCurrent == 0f -> 0f
            isActuallyCharging -> abs(rawCurrent)
            else -> -abs(rawCurrent)
        }

        val smoothedCurrent = if (lastCurrent == 0f) {
            normalizedCurrent
        } else {
            (lastCurrent * 0.8f + normalizedCurrent * 0.2f)
        }
        lastCurrent = smoothedCurrent

        // BARU: Hitung Watt
        // Rumus: P(W) = V(V) * I(A)
        // Arus dari sistem adalah dalam mikroampere (μA), jadi dibagi 1,000,000
        val wattage = voltage * (smoothedCurrent / 1_000_000f)

        // MODIFIKASI: Tambahkan wattage ke SystemStats
        updateNotification(SystemStats(
            batteryLevel = level,
            chargingCurrent = smoothedCurrent,
            voltage = voltage,
            wattage = wattage, // BARU
            batteryTemp = getBatteryTemperature(),
            chargingType = getChargingTypeFromPlugged(plugged),
            chargingStatus = getChargingStatusString(status, plugged),
            batteryHealth = getBatteryHealthString(health),
            healthPercentage = getBatteryHealthPercentage(),
            cycleCount = getBatteryCycleCount(),
            batteryCapacity = getBatteryCapacity(),
            currentCapacity = getCurrentCapacity(),
            batteryTechnology = technology,
            drain = calculateDrain(level, isActuallyCharging),
            idleDrain = calculateIdleDrain(level, isActuallyCharging),
            screenOnTime = getFormattedScreenOnTime(),
            screenOffTime = getFormattedScreenOffTime(),
            deepSleep = getDeepSleepPercentage(),
            awake = getAwakePercentage(),
            uptime = getFormattedUptime()
        ))
    }

    // ... (Fungsi-fungsi lain seperti getChargingStatusString, getBatteryHealthString, dll tetap sama) ...

    private fun getSystemStats(): SystemStats {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)?.toFloat()?.div(1000f) ?: 0f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val isActuallyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0
        val rawCurrent = getCurrentNow()

        val normalizedCurrent = when {
            rawCurrent == 0f -> 0f
            isActuallyCharging -> abs(rawCurrent)
            else -> -abs(rawCurrent)
        }

        val smoothedCurrent = if (lastCurrent == 0f) {
            normalizedCurrent
        } else {
            (lastCurrent * 0.8f + normalizedCurrent * 0.2f)
        }
        lastCurrent = smoothedCurrent

        // BARU: Hitung Watt di sini juga
        val wattage = voltage * (smoothedCurrent / 1_000_000f)

        // MODIFIKASI: Tambahkan wattage
        return SystemStats(
            batteryLevel = level,
            chargingCurrent = smoothedCurrent,
            voltage = voltage,
            wattage = wattage, // BARU
            batteryTemp = getBatteryTemperature(),
            chargingType = getChargingTypeFromPlugged(plugged),
            chargingStatus = getChargingStatusString(status, plugged),
            batteryHealth = getBatteryHealthString(health),
            healthPercentage = getBatteryHealthPercentage(),
            cycleCount = getBatteryCycleCount(),
            batteryCapacity = getBatteryCapacity(),
            currentCapacity = getCurrentCapacity(),
            batteryTechnology = technology,
            drain = calculateDrain(level, isActuallyCharging),
            idleDrain = calculateIdleDrain(level, isActuallyCharging),
            screenOnTime = getFormattedScreenOnTime(),
            screenOffTime = getFormattedScreenOffTime(),
            deepSleep = getDeepSleepPercentage(),
            awake = getAwakePercentage(),
            uptime = getFormattedUptime()
        )
    }

    // ... (Fungsi-fungsi lain seperti getBatteryTemperature, calculateDrain, dll tetap sama) ...

    private fun updateNotification(stats: SystemStats) {
        val notification = createNotification(
            batteryLevel = stats.batteryLevel,
            chargingCurrent = stats.chargingCurrent,
            voltage = stats.voltage,
            wattage = stats.wattage, // BARU
            batteryTemp = stats.batteryTemp,
            chargingType = stats.chargingType,
            chargingStatus = stats.chargingStatus,
            batteryTechnology = stats.batteryTechnology,
            drain = stats.drain,
            idleDrain = stats.idleDrain,
            screenOnTime = stats.screenOnTime,
            screenOffTime = stats.screenOffTime,
            deepSleep = stats.deepSleep,
            awake = stats.awake,
            uptime = stats.uptime
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // ... (Sisa fungsi lainnya tetap sama) ...


    // MODIFIKASI: Tambahkan wattage ke data class
    data class SystemStats(
        val batteryLevel: Int,
        val chargingCurrent: Float,
        val voltage: Float,
        val wattage: Float, // BARU
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
        val idleDrain: String,
        val screenOnTime: String,
        val screenOffTime: String,
        val deepSleep: String,
        val awake: String,
        val uptime: String
    )

    //
    // PASTE SEMUA FUNGSI LAINNYA YANG TIDAK BERUBAH DI SINI
    // Contoh: onBind, onDestroy, getChargingStatusString, dll.
    // Kode di atas hanya menunjukkan bagian yang dimodifikasi.
    // Pastikan Anda menyalin semua fungsi yang ada di file asli Anda.
    //
    // --- Di bawah ini adalah sisa fungsi yang tidak berubah ---
    //
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
        if (accessibleBatteryPaths.contains(path)) return true
        if (inaccessibleBatteryPaths.contains(path)) return false
        try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file.readText().trim()
                accessibleBatteryPaths.add(path)
                return true
            }
        } catch (e: Exception) {
            inaccessibleBatteryPaths.add(path)
            Log.d(TAG, "Path $path is not accessible: ${e.message}")
        }
        return false
    }

    private fun getBatteryHealthPercentage(): Int {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val designCapacity = getBatteryCapacityViaBatteryManager()
            val currentCapacity = getCurrentCapacityViaBatteryManager()
            if (designCapacity > 0 && currentCapacity > 0) {
                return ((currentCapacity.toFloat() / designCapacity) * 100).toInt()
            }
            val healthPaths = listOf(
                "/sys/class/power_supply/battery/health",
                "/sys/class/power_supply/battery/capacity_level",
                "/sys/class/power_supply/bms/battery_health"
            )
            for (path in healthPaths) {
                if (checkBatteryPathAccess(path)) {
                    val content = File(path).readText().trim()
                    val percentage = content.toIntOrNull()
                    if (percentage != null && percentage in 0..100) return percentage
                }
            }
            batteryPathsChecked = true
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery health percentage: ${e.message}")
        }
        return 100
    }

    private fun getBatteryCapacityViaBatteryManager(): Int {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val designCapacity = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (designCapacity != Long.MIN_VALUE && designCapacity > 0) {
                return (designCapacity / 1000).toInt()
            }
        } catch (e: Exception) {
            Log.d(TAG, "BatteryManager design capacity not available: ${e.message}")
        }
        return 0
    }

    private fun getCurrentCapacityViaBatteryManager(): Int {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentEnergy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (currentEnergy != Long.MIN_VALUE && currentEnergy > 0) {
                return (currentEnergy / 3700000).toInt()
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
                "/sys/class/power_supply/bms/cycle_count"
            )
            for (path in cyclePaths) {
                if (checkBatteryPathAccess(path)) {
                    val content = File(path).readText().trim()
                    return content.toIntOrNull() ?: 0
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery cycle count: ${e.message}")
        }
        return 0
    }

    private fun getBatteryCapacity(): Int {
        try {
            val capacityPaths = listOf(
                "/sys/class/power_supply/battery/charge_full_design",
                "/sys/class/power_supply/bms/charge_full_design"
            )
            for (path in capacityPaths) {
                if (checkBatteryPathAccess(path)) {
                    val cap = File(path).readText().trim().toLongOrNull()
                    if (cap != null && cap > 0) return (cap / 1000).toInt()
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
                "/sys/class/power_supply/bms/charge_full"
            )
            for (path in capacityPaths) {
                if (checkBatteryPathAccess(path)) {
                    val cap = File(path).readText().trim().toLongOrNull()
                    if (cap != null && cap > 0) return (cap / 1000).toInt()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting current capacity: ${e.message}")
        }
        return 0
    }

    private fun getBatteryTemperature(): Float {
        return try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            temperature / 10f
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery temperature", e)
            0f
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
            lastDrainTime = System.currentTimeMillis() // Reset time if level goes up slightly
            return "0.0%/h"
        }
        val timeDiffMillis = System.currentTimeMillis() - lastDrainTime
        if (timeDiffMillis < 60000 * 5) {
            return "Calculating..."
        }
        val drainPerHour = (levelDrop.toFloat() / timeDiffMillis.toFloat()) * (1000 * 60 * 60)
        return "%.1f%%/h".format(drainPerHour)
    }

    private fun calculateIdleDrain(currentLevel: Int, isCharging: Boolean): String {
        if (isCharging) return "Charging"
        if (idleDrainStartTime == 0L) return "Screen On"
        val levelDrop = idleDrainStartLevel - currentLevel
        if (levelDrop <= 0) return "0.0%/h"
        val timeDiffMillis = System.currentTimeMillis() - idleDrainStartTime
        if (timeDiffMillis < 60000 * 15) {
            return "Calculating..."
        }
        val drainPerHour = (levelDrop.toFloat() / timeDiffMillis.toFloat()) * (1000 * 60 * 60)
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
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val awakeMillis = android.os.SystemClock.uptimeMillis()
            val deepSleepMillis = uptimeMillis - awakeMillis
            val percentage = (deepSleepMillis.toFloat() / uptimeMillis.toFloat()) * 100
            "%.1f%%".format(percentage)
        } catch (e: Exception) {
            "0.0%"
        }
    }

    private fun getAwakePercentage(): String {
        return try {
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val awakeMillis = android.os.SystemClock.uptimeMillis()
            val awakePercentage = (awakeMillis.toFloat() / uptimeMillis.toFloat()) * 100
            "%.1f%%".format(awakePercentage)
        } catch (e: Exception) {
            "100.0%"
        }
    }

    private fun getFormattedUptime(): String {
        return try {
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val uptimeSeconds = uptimeMillis / 1000
            val days = uptimeSeconds / 86400
            val hours = (uptimeSeconds % 86400) / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60
            when {
                days > 0 -> "%dd %02dh %02dm".format(days, hours, minutes)
                else -> "%02dh %02dm %02ds".format(hours, minutes, seconds)
            }
        } catch (e: Exception) {
            "00h 00m 00s"
        }
    }

    private fun getCurrentNow(): Float {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (current != Int.MIN_VALUE) return current.toFloat()

            val possiblePaths = listOf(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/bms/current_now",
                "/sys/class/power_supply/usb/current_now"
            )
            for (path in possiblePaths) {
                if (checkBatteryPathAccess(path)) {
                    val file = File(path)
                    return file.readText().trim().toFloatOrNull() ?: 0f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentNow()", e)
        }
        return 0f
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "BatteryStatsService onDestroy")
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(screenStateReceiver)
            monitoringJob?.cancel()
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up service", e)
        }
        super.onDestroy()
        if (preferenceManager.getBatteryNotificationAutoStart()) {
            val restartIntent = Intent(applicationContext, BatteryStatsService::class.java)
            try {
                startForegroundService(restartIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service: ${e.message}")
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
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
}