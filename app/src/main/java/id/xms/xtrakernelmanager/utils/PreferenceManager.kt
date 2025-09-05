package id.xms.xtrakernelmanager.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("xkm_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BATTERY_STATS_ENABLED = "battery_stats_enabled"
        private const val KEY_SERVICE_AUTO_START = "service_auto_start"
    }

    fun setBatteryStatsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BATTERY_STATS_ENABLED, enabled)
            .apply()
    }

    fun getBatteryStatsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BATTERY_STATS_ENABLED, false)
    }

    fun setServiceAutoStart(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SERVICE_AUTO_START, enabled)
            .apply()
    }

    fun getServiceAutoStart(): Boolean {
        return sharedPreferences.getBoolean(KEY_SERVICE_AUTO_START, true)
    }
}
