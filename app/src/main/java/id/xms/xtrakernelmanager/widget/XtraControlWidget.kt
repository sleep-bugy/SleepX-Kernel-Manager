package id.xms.xtrakernelmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.service.BatteryStatsService
import id.xms.xtrakernelmanager.data.repository.TuningRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class XtraControlWidget : AppWidgetProvider() {

    @Inject
    lateinit var tuningRepository: TuningRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    companion object {
        const val ACTION_TOGGLE_BATTERY = "id.xms.xtrakernelmanager.TOGGLE_BATTERY"
        const val ACTION_CYCLE_PERFORMANCE = "id.xms.xtrakernelmanager.CYCLE_PERFORMANCE"
        private const val TAG = "XtraControlWidget"

        // Performance modes (same as PerformanceModeTileService)
        const val MODE_BATTERY_SAVER = 0
        const val MODE_BALANCED = 1
        const val MODE_PERFORMANCE = 2

        // Governor mappings
        private val GOVERNOR_MAP = mapOf(
            MODE_BATTERY_SAVER to "powersave",
            MODE_BALANCED to "schedutil",
            MODE_PERFORMANCE to "performance"
        )

        private val MODE_LABELS = mapOf(
            MODE_BATTERY_SAVER to "Battery Saver",
            MODE_BALANCED to "Balanced",
            MODE_PERFORMANCE to "Performance"
        )

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, XtraControlWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidgetStatic(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidgetStatic(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.xtra_control_widget)

            // Update battery notification status
            val batteryPrefs = context.getSharedPreferences("xkm_preferences", Context.MODE_PRIVATE)
            val batteryEnabled = batteryPrefs.getBoolean("battery_stats_enabled", false)

            if (batteryEnabled) {
                views.setTextViewText(R.id.battery_status_text, "Battery Stats: ON")
                views.setInt(R.id.battery_toggle_button, "setBackgroundResource", R.drawable.widget_button_enabled)
                views.setTextColor(R.id.battery_toggle_button, context.getColor(android.R.color.white))
            } else {
                views.setTextViewText(R.id.battery_status_text, "Battery Stats: OFF")
                views.setInt(R.id.battery_toggle_button, "setBackgroundResource", R.drawable.widget_button_disabled)
                views.setTextColor(R.id.battery_toggle_button, context.getColor(android.R.color.white))
            }

            // Update performance mode status
            val perfPrefs = context.getSharedPreferences("performance_mode_tile", Context.MODE_PRIVATE)
            val currentMode = perfPrefs.getInt("current_mode", MODE_BALANCED)
            val modeLabel = MODE_LABELS[currentMode] ?: "Balanced"

            views.setTextViewText(R.id.performance_status_text, "Mode: $modeLabel")

            // Set performance button appearance based on mode
            when (currentMode) {
                MODE_BATTERY_SAVER -> {
                    views.setInt(R.id.performance_cycle_button, "setBackgroundResource", R.drawable.widget_button_battery_saver)
                    views.setTextColor(R.id.performance_cycle_button, context.getColor(android.R.color.white))
                }
                MODE_BALANCED -> {
                    views.setInt(R.id.performance_cycle_button, "setBackgroundResource", R.drawable.widget_button_balanced)
                    views.setTextColor(R.id.performance_cycle_button, context.getColor(android.R.color.white))
                }
                MODE_PERFORMANCE -> {
                    views.setInt(R.id.performance_cycle_button, "setBackgroundResource", R.drawable.widget_button_performance)
                    views.setTextColor(R.id.performance_cycle_button, context.getColor(android.R.color.white))
                }
            }

            // Set up click listeners
            val batteryToggleIntent = Intent(context, XtraControlWidget::class.java).apply {
                action = ACTION_TOGGLE_BATTERY
            }
            val batteryPendingIntent = PendingIntent.getBroadcast(
                context, 0, batteryToggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.battery_toggle_button, batteryPendingIntent)

            val performanceCycleIntent = Intent(context, XtraControlWidget::class.java).apply {
                action = ACTION_CYCLE_PERFORMANCE
            }
            val performancePendingIntent = PendingIntent.getBroadcast(
                context, 1, performanceCycleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.performance_cycle_button, performancePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget $appWidgetId updated - Battery: $batteryEnabled, Performance: $modeLabel")
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidgetStatic(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Log.d(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            ACTION_TOGGLE_BATTERY -> {
                toggleBatteryNotification(context)
                updateAllWidgets(context)
            }
            ACTION_CYCLE_PERFORMANCE -> {
                cyclePerformanceMode(context)
                updateAllWidgets(context)
            }
        }
    }

    private fun toggleBatteryNotification(context: Context) {
        val prefs = context.getSharedPreferences("xkm_preferences", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("battery_stats_enabled", false)

        Log.d(TAG, "Toggling battery notification: $isEnabled -> ${!isEnabled}")

        if (isEnabled) {
            // Disable battery notification
            prefs.edit().putBoolean("battery_stats_enabled", false).apply()
            val serviceIntent = Intent(context, BatteryStatsService::class.java)
            context.stopService(serviceIntent)
        } else {
            // Enable battery notification
            prefs.edit().putBoolean("battery_stats_enabled", true).apply()
            val serviceIntent = Intent(context, BatteryStatsService::class.java)
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start BatteryStatsService", e)
                prefs.edit().putBoolean("battery_stats_enabled", false).apply()
            }
        }
    }

    private fun cyclePerformanceMode(context: Context) {
        val prefs = context.getSharedPreferences("performance_mode_tile", Context.MODE_PRIVATE)
        val currentMode = prefs.getInt("current_mode", MODE_BALANCED)
        val nextMode = when (currentMode) {
            MODE_BATTERY_SAVER -> MODE_BALANCED
            MODE_BALANCED -> MODE_PERFORMANCE
            MODE_PERFORMANCE -> MODE_BATTERY_SAVER
            else -> MODE_BALANCED
        }

        Log.d(TAG, "Cycling performance mode: $currentMode -> $nextMode")

        // Save new mode
        prefs.edit().putInt("current_mode", nextMode).apply()

        // Apply the corresponding governor using TuningRepository
        val governor = GOVERNOR_MAP[nextMode] ?: "schedutil"
        serviceScope.launch {
            cpuClusters.forEach { cluster ->
                try {
                    tuningRepository.setCpuGov(cluster, governor)
                    Log.d(TAG, "Applied governor $governor to cluster $cluster")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply governor to cluster $cluster", e)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.xtra_control_widget)

        // Update battery notification status
        val batteryPrefs = context.getSharedPreferences("xkm_preferences", Context.MODE_PRIVATE)
        val batteryEnabled = batteryPrefs.getBoolean("battery_stats_enabled", false)

        if (batteryEnabled) {
            views.setTextViewText(R.id.battery_status_text, "Battery Stats: ON")
            views.setInt(R.id.battery_toggle_button, "setBackgroundResource", R.drawable.widget_button_enabled)
            views.setTextColor(R.id.battery_toggle_button, context.getColor(android.R.color.white))
        } else {
            views.setTextViewText(R.id.battery_status_text, "Battery Stats: OFF")
            views.setInt(R.id.battery_toggle_button, "setBackgroundResource", R.drawable.widget_button_disabled)
            views.setTextColor(R.id.battery_toggle_button, context.getColor(android.R.color.white))
        }

        // Update performance mode status
        val perfPrefs = context.getSharedPreferences("performance_mode_tile", Context.MODE_PRIVATE)
        val currentMode = perfPrefs.getInt("current_mode", MODE_BALANCED)
        val modeLabel = MODE_LABELS[currentMode] ?: "Balanced"

        views.setTextViewText(R.id.performance_status_text, "Mode: $modeLabel")

        // Set performance button appearance based on mode
        when (currentMode) {
            MODE_BATTERY_SAVER -> {
                views.setInt(R.id.performance_cycle_button, "setBackgroundResource", R.drawable.widget_button_battery_saver)
                views.setTextColor(R.id.performance_cycle_button, context.getColor(android.R.color.white))
            }
            MODE_BALANCED -> {
                views.setInt(R.id.performance_cycle_button, "setBackgroundResource", R.drawable.widget_button_balanced)
                views.setTextColor(R.id.performance_cycle_button, context.getColor(android.R.color.black))
            }
            MODE_PERFORMANCE -> {
                views.setInt(R.id.performance_cycle_button, "setBackgroundResource", R.drawable.widget_button_performance)
                views.setTextColor(R.id.performance_cycle_button, context.getColor(android.R.color.white))
            }
        }

        // Set up click listeners
        val batteryToggleIntent = Intent(context, XtraControlWidget::class.java).apply {
            action = ACTION_TOGGLE_BATTERY
        }
        val batteryPendingIntent = PendingIntent.getBroadcast(
            context, 0, batteryToggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.battery_toggle_button, batteryPendingIntent)

        val performanceCycleIntent = Intent(context, XtraControlWidget::class.java).apply {
            action = ACTION_CYCLE_PERFORMANCE
        }
        val performancePendingIntent = PendingIntent.getBroadcast(
            context, 1, performanceCycleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.performance_cycle_button, performancePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.d(TAG, "Widget $appWidgetId updated - Battery: $batteryEnabled, Performance: $modeLabel")
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "Widget enabled for the first time")
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Last widget disabled")
    }
}
