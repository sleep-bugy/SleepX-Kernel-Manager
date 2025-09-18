package id.xms.xtrakernelmanager.service

import android.app.*
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
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.MainActivity
import id.xms.xtrakernelmanager.utils.PreferenceManager
import kotlinx.coroutines.*
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

    private var lastCurrent = 0f

    // Variabel untuk Drain Berbasis Charge Counter
    private var designCapacityUah: Long = 4000000L
    private var lastActiveChargeCounter: Long = 0L
    private var lastIdleChargeCounter: Long = 0L
    private var lastActiveDrainTime: Long = 0L
    private var lastIdleDrainTime: Long = 0L
    private var lastIdleDrainValue: String = "N/A"

    // Variabel untuk Screen Time
    private var screenOnSince = 0L
    private var screenOnTime = 0L

    companion object {
        const val MAIN_NOTIFICATION_ID = 2
        const val MAIN_CHANNEL_ID = "battery_stats_channel"
        const val TEMP_NOTIFICATION_ID = 3
        const val TEMP_CHANNEL_ID = "temperature_stats_channel"

        private const val UPDATE_INTERVAL_MS = 2000L
        private const val MIN_DRAIN_TIME_MS = 5 * 60 * 1000L
    }

    private val systemStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0) {
                        lastIdleChargeCounter = 0
                        lastIdleDrainTime = 0
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (screenOnSince == 0L) screenOnSince = System.currentTimeMillis()
                    lastIdleChargeCounter = 0
                    lastIdleDrainTime = 0
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (screenOnSince > 0L) screenOnTime += System.currentTimeMillis() - screenOnSince
                    screenOnSince = 0L
                    lastIdleChargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                    lastIdleDrainTime = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        designCapacityUah = getDesignCapacityUah()
        preferenceManager.setBatteryStatsEnabled(true)

        createMainNotificationChannel()
        createTempNotificationChannel()

        startForeground(MAIN_NOTIFICATION_ID, createMainNotification())

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(systemStateReceiver, intentFilter)
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun getDesignCapacityUah(): Long {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val capacityPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val chargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (capacityPct > 0 && chargeCounter > 0) {
            val calculatedCapacity = (chargeCounter * 100) / capacityPct
            if (calculatedCapacity > 1000000) return calculatedCapacity
        }
        return 4000000L
    }

    private fun createMainNotificationChannel() {
        val channel = NotificationChannel(MAIN_CHANNEL_ID, "Battery & System Stats", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun createTempNotificationChannel() {
        val channel = NotificationChannel(TEMP_CHANNEL_ID, "Temperature Stats", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun createBatteryPercentIcon(percent: Int): IconCompat {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        paint.color = Color.DKGRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3f, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = size * 0.55f
        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(percent.toString(), size / 2f, yPos, paint)

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun createTemperatureIcon(temperature: Float): IconCompat {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }


        // Draw temperature with °C
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = size * 0.48f
        val tempText = temperature.toInt().toString() + "°C"
        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(tempText, size / 2f, yPos, paint)

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun createMainNotification(stats: SystemStats = SystemStats(), showTempInStatusBar: Boolean = false): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val formattedCurrent = "%.0fmA".format(stats.chargingCurrent / 1000)
        val formattedWattage = "%.2fW".format(stats.wattage)
        val formattedTemp = "%.1f°C".format(stats.batteryTemp)

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("Battery & System Statistics")
            .bigText(
                """
                Power: $formattedCurrent / $formattedWattage
                Drain: Active: ${stats.activeDrain} • Idle: ${stats.idleDrain}
                Screen On: ${stats.screenOnTime} • Screen Off: ${stats.screenOffTime}
                Uptime: ${stats.uptime}
                Awake: ${stats.awake}
                Deep Sleep: ${stats.deepSleep}
                Technologies: ${stats.chargeTechnology}""".trimIndent()
            )

        return if (showTempInStatusBar) {
            NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
                .setContentTitle("🌡️ $formattedTemp • ${stats.chargingStatus}")
                .setContentText("⚡ $formattedCurrent • Drain: ${stats.activeDrain}")
                .setSmallIcon(createTemperatureIcon(stats.batteryTemp))
                .setStyle(bigTextStyle)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .setOngoing(true)
                .build()
        } else {
            NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
                .setContentTitle("🔋 ${stats.batteryLevel}% • ${stats.chargingStatus}")
                .setContentText("⚡ $formattedCurrent • Drain: ${stats.activeDrain}")
                .setSmallIcon(createBatteryPercentIcon(stats.batteryLevel))
                .setStyle(bigTextStyle)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .setOngoing(true)
                .build()
        }
    }

    private fun createTempNotification(stats: SystemStats = SystemStats()): Notification {
        val formattedTemp = "%.1f°C".format(stats.batteryTemp)
        return NotificationCompat.Builder(this, TEMP_CHANNEL_ID)
            .setContentTitle("Battery Temperature: $formattedTemp")
            .setSmallIcon(createTemperatureIcon(stats.batteryTemp))
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                val stats = getSystemStats()
                updateNotifications(stats)
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateNotifications(stats: SystemStats) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val showTempInStatusBar = preferenceManager.getShowBatteryTempInStatusBar()
        notificationManager.notify(MAIN_NOTIFICATION_ID, createMainNotification(stats, showTempInStatusBar))
        if (!showTempInStatusBar) {
            notificationManager.notify(TEMP_NOTIFICATION_ID, createTempNotification(stats))
        } else {
            notificationManager.cancel(TEMP_NOTIFICATION_ID)
        }
    }

    private fun getSystemStats(): SystemStats {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0

        val rawCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat()
        val normalizedCurrent = if (isCharging) abs(rawCurrent) else -abs(rawCurrent)
        val smoothedCurrent = if (lastCurrent == 0f) normalizedCurrent else (lastCurrent * 0.8f + normalizedCurrent * 0.2f)
        lastCurrent = smoothedCurrent

        val voltage = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
        val currentChargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        val uptimeMillis = SystemClock.uptimeMillis()
        val elapsedRealtimeMillis = SystemClock.elapsedRealtime()
        val screenOffTimeMillis = elapsedRealtimeMillis - uptimeMillis
        val deepSleepMillis = elapsedRealtimeMillis - uptimeMillis // Approximation

        val chargeTechnology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"


        return SystemStats(
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            chargingCurrent = smoothedCurrent,
            wattage = voltage * (smoothedCurrent / 1_000_000f),
            batteryTemp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
            chargingStatus = getChargingStatusString(status, plugged),
            activeDrain = calculateActiveDrain(currentChargeCounter, isCharging),
            idleDrain = calculateIdleDrain(currentChargeCounter, isCharging),
            screenOnTime = getFormattedScreenOnTime(),
            uptime = formatMillisToTime(uptimeMillis),
            awake = formatMillisToTime(elapsedRealtimeMillis),
            screenOffTime = formatMillisToTime(screenOffTimeMillis),
            deepSleep = formatMillisToTime(deepSleepMillis),
            chargeTechnology = chargeTechnology
        )
    }

    private fun calculateActiveDrain(currentCharge: Long, isCharging: Boolean): String {
        if (isCharging) {
            lastActiveChargeCounter = 0
            return "Charging"
        }
        if (lastActiveChargeCounter == 0L) {
            lastActiveChargeCounter = currentCharge
            lastActiveDrainTime = System.currentTimeMillis()
            return "Calculating..."
        }

        val timeDiff = System.currentTimeMillis() - lastActiveDrainTime
        if (timeDiff < MIN_DRAIN_TIME_MS) return "Calculating..."

        val chargeDropUah = lastActiveChargeCounter - currentCharge
        if (chargeDropUah <= 0) return "0.0%/h"

        val chargeDropPct = (chargeDropUah.toDouble() / designCapacityUah) * 100
        val drainRatePctPerHour = (chargeDropPct / timeDiff) * (1000 * 60 * 60)

        lastActiveChargeCounter = currentCharge
        lastActiveDrainTime = System.currentTimeMillis()

        return "%.1f%%/h".format(drainRatePctPerHour)
    }

    private fun calculateIdleDrain(currentCharge: Long, isCharging: Boolean): String {
        if (isCharging) return "Charging"
        if (lastIdleChargeCounter == 0L) return lastIdleDrainValue

        val timeDiff = System.currentTimeMillis() - lastIdleDrainTime
        if (timeDiff < MIN_DRAIN_TIME_MS) return "Calculating..."

        val chargeDropUah = lastIdleChargeCounter - currentCharge
        if (chargeDropUah <= 0) return "0.0%/h"

        val chargeDropPct = (chargeDropUah.toDouble() / designCapacityUah) * 100
        val drainRatePctPerHour = (chargeDropPct / timeDiff) * (1000 * 60 * 60)

        val result = "%.1f%%/h".format(drainRatePctPerHour)
        lastIdleDrainValue = result
        return result
    }

    private fun getChargingStatusString(status: Int, plugged: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (plugged > 0) "Not Charging" else "Discharging"
        else -> "Unknown"
    }

    private fun getFormattedScreenOnTime(): String {
        var currentScreenOnTime = screenOnTime
        if (screenOnSince > 0) currentScreenOnTime += System.currentTimeMillis() - screenOnSince
        val totalSeconds = currentScreenOnTime / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatMillisToTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(systemStateReceiver)
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (preferenceManager.getServiceAutoStart()) {
            val restartIntent = Intent(applicationContext, this::class.java)
            startForegroundService(restartIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    data class SystemStats(
        val batteryLevel: Int = 0,
        val chargingCurrent: Float = 0f,
        val wattage: Float = 0f,
        val batteryTemp: Float = 0f,
        val chargingStatus: String = "N/A",
        val activeDrain: String = "N/A",
        val idleDrain: String = "N/A",
        val screenOnTime: String = "00:00:00",
        val uptime: String = "00:00:00",
        val awake: String = "00:00:00",
        val screenOffTime: String = "00:00:00",
        val deepSleep: String = "00:00:00",
        val chargeTechnology: String = "N/A"
    )
}