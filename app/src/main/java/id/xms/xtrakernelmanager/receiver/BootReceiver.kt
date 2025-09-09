// File: id/xms/xtrakernelmanager/receiver/BootReceiver.kt

package id.xms.xtrakernelmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import id.xms.xtrakernelmanager.service.BatteryStatsService
import id.xms.xtrakernelmanager.utils.PreferenceManager

// Hapus @AndroidEntryPoint dan @Inject karena tidak akan bekerja di sini
class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot event received: ${intent.action}")

        // Hanya dengarkan action yang paling relevan dan andal
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,        // Perangkat selesai boot setelah user unlock
            Intent.ACTION_MY_PACKAGE_REPLACED -> { // Aplikasi baru saja di-update

                // Buat instance PreferenceManager secara manual di sini
                val preferenceManager = PreferenceManager(context)

                // Cek apakah layanan seharusnya berjalan
                if (preferenceManager.getBatteryStatsEnabled()) {
                    Log.d(TAG, "Starting BatteryStatsService...")
                    startService(context)
                } else {
                    Log.d(TAG, "Battery stats service is disabled in preferences.")
                }
            }
        }
    }

    private fun startService(context: Context) {
        val serviceIntent = Intent(context, BatteryStatsService::class.java)
        try {
            // Selalu gunakan startForegroundService dari background
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "BatteryStatsService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BatteryStatsService: ${e.message}", e)
        }
    }
}