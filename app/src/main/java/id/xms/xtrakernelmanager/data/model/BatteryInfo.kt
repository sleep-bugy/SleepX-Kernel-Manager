package id.xms.xtrakernelmanager.data.model

data class BatteryInfo(
    val level: Int = 0,
    val temp: Float = 0f,
    val voltage: Float = 0f,
    val isCharging: Boolean = false,
    val current: Float = 0f,
    val chargingWattage: Float = 0f,
    val technology: String = "",
    val health: String = "",
    val status: String = "",
    val chargingType: String = ""
)
