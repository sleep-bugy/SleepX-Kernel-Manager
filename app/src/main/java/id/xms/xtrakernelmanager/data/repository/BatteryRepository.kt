package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import id.xms.xtrakernelmanager.data.model.BatteryInfo
import kotlin.math.abs

@Singleton
class BatteryRepository @Inject constructor(
    private val context: Context
) {
    fun getBatteryInfo(): Flow<BatteryInfo> = flow {
        while (true) {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0f
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

                // Determine chargingType from plugged
                val chargingType = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "Battery"
                }

                // Determine isCharging using only status
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                val isFull = status == BatteryManager.BATTERY_STATUS_FULL

                // Get charging current (may be in microamperes and vendor-dependent sign)
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val rawCurrentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

                // Convert to mA (float). If provider returns Int.MIN_VALUE treat as 0.
                var currentMa = if (rawCurrentUa == Int.MIN_VALUE) 0f else rawCurrentUa.toFloat() / 1000f

                // Normalize sign using plugged as a heuristic: if unplugged but current reported positive, make negative (discharging).
                currentMa = when {
                    plugged == 0 && currentMa > 0f -> -currentMa
                    plugged != 0 && currentMa < 0f -> -currentMa
                    else -> currentMa
                }

                // Compute wattage from voltage (V) and current (mA -> A)
                val chargingWattage = abs(voltage * (currentMa / 1000f))

                val batteryInfo = BatteryInfo(
                    level = if (scale > 0) ((level * 100 / scale.toFloat()).toInt()) else level,
                    temp = temp,
                    voltage = voltage,
                    isCharging = isCharging,
                    current = currentMa,
                    chargingWattage = if (isCharging) chargingWattage else 0f,
                    technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown",
                    health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failed"
                        else -> "Unknown"
                    },
                    status = when (status) {
                        BatteryManager.BATTERY_STATUS_FULL -> "Full"
                        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (plugged == 0) "Not Charging" else "Charging"
                        else -> if (plugged != 0) "Charging" else "Unknown"
                    },
                    plugged = plugged,
                    chargingType = chargingType
                )

                emit(batteryInfo)
            }
            kotlinx.coroutines.delay(1000)
        }
    }.flowOn(Dispatchers.IO)
}
