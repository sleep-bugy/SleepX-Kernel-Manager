package id.xms.xtrakernelmanager.service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.MainActivity
import id.xms.xtrakernelmanager.utils.PreferenceManager
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class BatteryStatsService : Service() {

    @Inject lateinit var preferenceManager: PreferenceManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null

    private var lastCurrent = 0f
    private var designCapacityUah: Long = 0L


    // Idle baseline
    private var idleStartLevel = -1
    private var idleStartCharge = 0L
    private var idleStartTime = 0L
    private var lastIdleDrainResult: String = "N/A"

// Realtime usage
// ...

    // Realtime usage
    private var realtimeUsedMah = 0.0
    private var lastRealtimeUpdate = System.currentTimeMillis()

    // Screen time
    private var screenOnSince = 0L
    private var screenOnTime = 0L

    companion object {
        private const val MAIN_NOTIFICATION_ID = 2
        private const val MAIN_CHANNEL_ID = "battery_stats_channel"
        private const val TEMP_NOTIFICATION_ID = 3
        private const val TEMP_CHANNEL_ID = "temperature_stats_channel"

        private const val UPDATE_INTERVAL_MS = 2000L
        private const val MIN_DRAIN_TIME_MS = 30 * 1000L // 30 seconds for quick testing
    }

    // === Lifecycle ===
    override fun onCreate() {
        super.onCreate()
        designCapacityUah = getDesignCapacityUah()
        Log.d("BatteryDebug", "Design capacity = ${designCapacityUah / 1000} mAh")

        createNotificationChannels()
        startForeground(MAIN_NOTIFICATION_ID, buildInitNotification())
        preferenceManager.setBatteryStatsEnabled(true)

        // Register receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(systemReceiver, filter)

        // Set idle baseline if already idle at service start
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0)
        if (!isCharging && !isScreenOn) {
            idleStartCharge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            idleStartLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            idleStartTime = System.currentTimeMillis()
            Log.d("BatteryDebug", "Idle baseline set (service start, already idle) -> charge=$idleStartCharge, level=$idleStartLevel")
        }

        startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(systemReceiver)
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (preferenceManager.getServiceAutoStart()) {
            startForegroundService(Intent(applicationContext, this::class.java))
        }
    }

    // === Broadcast Receiver ===
    // File: BatteryStatsService.kt

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    screenOnSince = System.currentTimeMillis()

                    // Lakukan kalkulasi final SEBELUM mereset baseline
                    if (idleStartTime > 0L) {
                        val currentCharge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                        val currentLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        // Panggil calculateIdleDrain untuk mendapatkan hasil akhir sesi
                        lastIdleDrainResult = calculateIdleDrain(currentCharge, currentLevel, isCharging = false)
                        Log.d("BatteryDebug", "Idle session ended. Final result: $lastIdleDrainResult")
                    }

                    // Sekarang baru reset baseline
                    resetIdleDrainBaseline("screen on")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    val batteryStatusIntent = context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val plugged = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                    val isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0)

                    if (!isCharging) {
                        idleStartCharge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                        idleStartLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        idleStartTime = System.currentTimeMillis()
                        Log.d("BatteryDebug", "Idle baseline set (screen off, not charging) -> charge=$idleStartCharge, level=$idleStartLevel")
                    }
                }
            }
        }
    }

    private fun resetIdleDrainBaseline(reason: String) {
        idleStartLevel = -1
        idleStartCharge = 0L
        idleStartTime = 0L
        Log.d("BatteryDebug", "Idle drain baseline reset ($reason)")
    }

    // === Monitoring Loop ===
    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            while (isActive) {
                updateRealtimeUsed(bm)
                val stats = collectSystemStats(bm)
                updateNotifications(stats)
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    // === Data Collection ===
    private fun collectSystemStats(bm: BatteryManager): SystemStats {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0)

        val rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat()
        val normalizedCurrent = if (isCharging) abs(rawCurrent) else -abs(rawCurrent)
        val smoothedCurrent = if (lastCurrent == 0f) normalizedCurrent else (lastCurrent * 0.8f + normalizedCurrent * 0.2f)
        lastCurrent = smoothedCurrent

        val voltage = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
        val chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        val uptimeMillis = SystemClock.uptimeMillis()
        val elapsedRealtimeMillis = SystemClock.elapsedRealtime()
        val screenOffTimeMillis = elapsedRealtimeMillis - uptimeMillis

        return SystemStats(
            batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            chargingCurrent = smoothedCurrent,
            wattage = voltage * (smoothedCurrent / 1_000_000f),
            batteryTemp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
            chargingStatus = getChargingStatusString(status, plugged),
            activeDrain = calculateActiveDrain(smoothedCurrent, isCharging),
            idleDrain = calculateIdleDrain(chargeCounter, bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY), isCharging),
            realtimeUsed = "%.0f mAh".format(realtimeUsedMah),
            screenOnTime = formatMillis(screenOnTime + if (screenOnSince > 0) System.currentTimeMillis() - screenOnSince else 0),
            uptime = formatMillis(uptimeMillis),
            awake = formatMillis(elapsedRealtimeMillis),
            screenOffTime = formatMillis(screenOffTimeMillis),
            deepSleep = formatMillis(screenOffTimeMillis), // masih approx
            chargeTechnology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        )
    }

    // === Calculations ===
    private fun calculateActiveDrain(currentUa: Float, isCharging: Boolean): String {
        if (isCharging) return "Charging"
        val currentMa = abs(currentUa) / 1000f
        val capacityMah = designCapacityUah / 1000f
        if (capacityMah <= 0 || currentMa == 0f) return "0.0%/h"
        val rate = (currentMa / capacityMah) * 100
        return "%.1f%%/h (≈%.0f mAh)".format(rate, currentMa)
    }

    private fun calculateIdleDrain(currentCharge: Long, currentLevel: Int, isCharging: Boolean): String {
        // 1. If there's no active idle session, return the last saved result.
        if (idleStartTime == 0L) return lastIdleDrainResult

        // 2. If charging, the idle session is interrupted.
        if (isCharging) {
            // We consider the idle session over and reset the baseline.
            resetIdleDrainBaseline("charging started")
            return "Charging"
        }
        val elapsedMs = System.currentTimeMillis() - idleStartTime
        // Wait for a minimum duration before showing a result.
        if (elapsedMs < MIN_DRAIN_TIME_MS) return "Calculating..."

        // --- MODIFICATION START ---

        // 3. Prioritize CHARGE_COUNTER calculation if the values are valid.
        //    A valid charge counter will be a positive number.
        val useChargeCounter = idleStartCharge > 0 && currentCharge > 0
        if (useChargeCounter) {
            val dropUah = idleStartCharge - currentCharge
            if (dropUah > 0) {
                val dropPct = (dropUah.toDouble() / designCapacityUah) * 100
                val rate = dropPct / (elapsedMs / 3600000.0) // 3.6e6 ms in an hour
                val rateMah = (dropUah / 1000.0) / (elapsedMs / 3600000.0)
                return "%.2f%%/h (≈%.1f mAh/h)".format(rate, rateMah)
            }
        }

        // 4. Fallback to percentage-based calculation if CHARGE_COUNTER is not supported
        //    or if no drop was detected with it.
        if (idleStartLevel > 0 && currentLevel > 0) {
            val dropPct = (idleStartLevel - currentLevel).toDouble()
            if (dropPct > 0) {
                val rate = dropPct / (elapsedMs / 3600000.0)
                // Estimate mAh drain based on percentage drop and design capacity
                val rateMah = (rate / 100) * (designCapacityUah / 1000.0)
                return "%.2f%%/h (≈%.1f mAh/h)".format(rate, rateMah)
            }
        }
        // 5. If no drop is detected by any method, the drain is effectively zero.
        return "0.0%/h"
    }


    private fun updateRealtimeUsed(bm: BatteryManager) {
        val now = System.currentTimeMillis()
        val deltaSec = (now - lastRealtimeUpdate) / 1000.0
        lastRealtimeUpdate = now

        val currentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (currentUa == Int.MIN_VALUE) return

        val dischargeMa = -currentUa / 1000.0
        if (dischargeMa > 0) {
            realtimeUsedMah += dischargeMa * (deltaSec / 3600.0)
            Log.d("BatteryDebug", "Realtime used += %.2f mAh (total %.0f mAh)".format(dischargeMa * (deltaSec / 3600.0), realtimeUsedMah))
        }
    }

    // === Notifications ===
    private fun updateNotifications(stats: SystemStats) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val showTemp = preferenceManager.getShowBatteryTempInStatusBar()
        nm.notify(MAIN_NOTIFICATION_ID, buildMainNotification(stats, showTemp))
        if (!showTemp) nm.notify(TEMP_NOTIFICATION_ID, buildTempNotification(stats))
        else nm.cancel(TEMP_NOTIFICATION_ID)
    }

    private fun buildInitNotification(): Notification =
        NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
            .setContentTitle("Battery Stats Running")
            .setContentText("Collecting battery statistics...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun buildMainNotification(stats: SystemStats, showTemp: Boolean): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val formattedCurrent = "%.0fmA".format(stats.chargingCurrent / 1000)
        val formattedWattage = "%.2fW".format(stats.wattage)
        val formattedTemp = "%.1f°C".format(stats.batteryTemp)

        val style = NotificationCompat.BigTextStyle()
            .setBigContentTitle("Battery & System Statistics")
            .bigText(
                """
                Power: $formattedCurrent / $formattedWattage
                Active Drain: ${stats.activeDrain}
                Idle Drain: ${stats.idleDrain}
                Realtime Used: ${stats.realtimeUsed}
                Screen On: ${stats.screenOnTime} • Screen Off: ${stats.screenOffTime}
                Uptime: ${stats.uptime}
                Awake: ${stats.awake}
                Deep Sleep: ${stats.deepSleep}
                Technologies: ${stats.chargeTechnology}
                """.trimIndent()
            )

        val builder = NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
            .setStyle(style)
            .setContentIntent(pi)
            .setSilent(true)
            .setOngoing(true)

        return if (showTemp) {
            builder.setContentTitle("🌡️ $formattedTemp • ${stats.chargingStatus}")
                .setContentText("⚡ $formattedCurrent • Drain: ${stats.activeDrain}")
                .setSmallIcon(createTemperatureIcon(stats.batteryTemp))
                .build()
        } else {
            builder.setContentTitle("🔋 ${stats.batteryLevel}% • ${stats.chargingStatus}")
                .setContentText("⚡ $formattedCurrent • Drain: ${stats.activeDrain}")
                .setSmallIcon(createBatteryPercentIcon(stats.batteryLevel))
                .build()
        }
    }

    private fun buildTempNotification(stats: SystemStats): Notification {
        val formattedTemp = "%.1f°C".format(stats.batteryTemp)
        return NotificationCompat.Builder(this, TEMP_CHANNEL_ID)
            .setContentTitle("Battery Temperature: $formattedTemp")
            .setSmallIcon(createTemperatureIcon(stats.batteryTemp))
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(MAIN_CHANNEL_ID, "Battery & System Stats", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(TEMP_CHANNEL_ID, "Temperature Stats", NotificationManager.IMPORTANCE_LOW))
    }

    // === Utils ===
    private fun getChargingStatusString(status: Int, plugged: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (plugged > 0) "Not Charging" else "Discharging"
        else -> "Unknown"
    }

    private fun getDesignCapacityUah(): Long {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val capPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (capPct > 0 && chargeCounter > 0) return (chargeCounter * 100) / capPct

        listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/charge_full"
        ).forEach { path ->
            try {
                val value = File(path).readText().trim().toLong()
                if (value > 0) return value
            } catch (_: Exception) {}
        }
        return 4000000L
    }

    @SuppressLint("UseKtx")
    private fun createBatteryPercentIcon(percent: Int): IconCompat {
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        paint.color = Color.DKGRAY; paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3f, paint)
        paint.color = Color.WHITE; paint.style = Paint.Style.FILL; paint.textSize = size * 0.55f
        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(percent.toString(), size / 2f, yPos, paint)
        return IconCompat.createWithBitmap(bmp)
    }

    @SuppressLint("UseKtx")
    private fun createTemperatureIcon(temp: Float): IconCompat {
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        paint.color = Color.WHITE; paint.textSize = size * 0.48f
        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText("${temp.toInt()}°C", size / 2f, yPos, paint)
        return IconCompat.createWithBitmap(bmp)
    }

    private fun formatMillis(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    data class SystemStats(
        val batteryLevel: Int,
        val chargingCurrent: Float,
        val wattage: Float,
        val batteryTemp: Float,
        val chargingStatus: String,
        val activeDrain: String,
        val idleDrain: String,
        val realtimeUsed: String,
        val screenOnTime: String,
        val uptime: String,
        val awake: String,
        val screenOffTime: String,
        val deepSleep: String,
        val chargeTechnology: String
    )
}
