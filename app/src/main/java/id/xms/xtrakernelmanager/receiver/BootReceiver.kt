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

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {

                Log.d("BootReceiver", "Received ${intent.action}")

                // Check if battery stats service was enabled before reboot/update
                val isServiceEnabled = preferenceManager.getBatteryStatsEnabled()

                if (isServiceEnabled) {
                    Log.d("BootReceiver", "Starting BatteryStatsService after ${intent.action}")
                    startBatteryStatsService(context)
                }
            }
        }
    }

    private fun startBatteryStatsService(context: Context) {
        try {
            val serviceIntent = Intent(context, BatteryStatsService::class.java)
            context.startForegroundService(serviceIntent)
            Log.d("BootReceiver", "BatteryStatsService started successfully")
        } catch (e: Exception) {
            Log.e("BootReceiver", "Failed to start BatteryStatsService", e)
        }
    }
}
