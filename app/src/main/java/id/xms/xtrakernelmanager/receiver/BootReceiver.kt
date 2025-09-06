package id.xms.xtrakernelmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.service.BatteryStatsService
import id.xms.xtrakernelmanager.utils.PreferenceManager
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot event received: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                handleBootCompleted(context)
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Handling boot completed event")

        // Start BatteryStatsService on boot if it was previously enabled
        if (preferenceManager.getBatteryStatsEnabled()) {
            Log.d(TAG, "Starting BatteryStatsService on boot")

            val serviceIntent = Intent(context, BatteryStatsService::class.java)
            try {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "BatteryStatsService started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start BatteryStatsService: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Battery stats service was not previously enabled")
        }
    }
}
