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
import id.xms.xtrakernelmanager.utils.PreferenceManager
import javax.inject.Inject

@AndroidEntryPoint
class BatteryNotificationWidget : AppWidgetProvider() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    companion object {
        const val ACTION_TOGGLE_BATTERY_NOTIFICATION = "id.xms.xtrakernelmanager.TOGGLE_BATTERY_NOTIFICATION"
        private const val TAG = "BatteryNotificationWidget"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BatteryNotificationWidget::class.java)
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
            val prefs = context.getSharedPreferences("xkm_preferences", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("battery_stats_enabled", false)

            val views = RemoteViews(context.packageName, R.layout.battery_notification_widget)

            // Update button text and appearance based on current state
            if (isEnabled) {
                views.setTextViewText(R.id.widget_button, context.getString(R.string.widget_battery_stats_on))
                views.setInt(R.id.widget_button, "setBackgroundResource", R.drawable.widget_button_enabled)
                views.setTextColor(R.id.widget_button, context.getColor(android.R.color.white))
            } else {
                views.setTextViewText(R.id.widget_button, context.getString(R.string.widget_battery_stats_off))
                views.setInt(R.id.widget_button, "setBackgroundResource", R.drawable.widget_button_disabled)
                views.setTextColor(R.id.widget_button, context.getColor(android.R.color.white))
            }

            // Set up click listener
            val toggleIntent = Intent(context, BatteryNotificationWidget::class.java).apply {
                action = ACTION_TOGGLE_BATTERY_NOTIFICATION
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_button, togglePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget $appWidgetId updated - Battery stats: $isEnabled")
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        // Update all widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidgetStatic(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Log.d(TAG, "onReceive: ${intent.action}")

        if (intent.action == ACTION_TOGGLE_BATTERY_NOTIFICATION) {
            toggleBatteryNotification(context)

            // Update all widgets after toggle
            updateAllWidgets(context)
        }
    }

    private fun toggleBatteryNotification(context: Context) {
        val prefs = context.getSharedPreferences("xkm_preferences", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("battery_stats_enabled", false)

        Log.d(TAG, "Current battery notification state: $isEnabled")

        if (isEnabled) {
            // Disable battery notification
            Log.d(TAG, "Disabling battery notification")
            prefs.edit().putBoolean("battery_stats_enabled", false).apply()
            val serviceIntent = Intent(context, BatteryStatsService::class.java)
            context.stopService(serviceIntent)
        } else {
            // Enable battery notification
            Log.d(TAG, "Enabling battery notification")
            prefs.edit().putBoolean("battery_stats_enabled", true).apply()
            val serviceIntent = Intent(context, BatteryStatsService::class.java)
            try {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "BatteryStatsService started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start BatteryStatsService", e)
                // Revert preference on failure
                prefs.edit().putBoolean("battery_stats_enabled", false).apply()
            }
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "Widget enabled for the first time")
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Last widget disabled")
    }
}
