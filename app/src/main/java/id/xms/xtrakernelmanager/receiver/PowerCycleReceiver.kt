package id.xms.xtrakernelmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class PowerCycleReceiver : BroadcastReceiver() {

    companion object {
        const val SOT_PREFS = "sot_prefs"
        const val KEY_SOT_RESET_TIMESTAMP = "sot_reset_timestamp"
        private const val CHARGE_THRESHOLD_PERCENT = 90
        private const val TAG = "PowerCycleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_DISCONNECTED) {
            return
        }

        // We only care when the power is disconnected.
        Log.d(TAG, "Power disconnected event received.")

        // Get the current battery level at the moment of disconnection.
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            context.registerReceiver(null, it)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        if (level == -1 || scale == -1) {
            Log.e(TAG, "Could not read battery level.")
            return
        }

        val batteryPct = (level / scale.toFloat() * 100).toInt()
        Log.d(TAG, "Battery level at disconnection: $batteryPct%")

        // If the battery was charged above our threshold, we reset the SOT counter.
        if (batteryPct >= CHARGE_THRESHOLD_PERCENT) {
            Log.i(TAG, "Charge cycle reset triggered. Battery is at $batteryPct%. Storing new SOT timestamp.")
            val prefs = context.getSharedPreferences(SOT_PREFS, Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putLong(KEY_SOT_RESET_TIMESTAMP, System.currentTimeMillis())
                apply()
            }
        }
    }
}
