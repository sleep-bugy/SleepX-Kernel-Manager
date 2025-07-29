package id.xms.xtrakernelmanager.data.model
import kotlinx.serialization.Serializable

@Serializable
data class BatteryInfo(
    val level: Int,
    val temp: Float,
    val health: Int,
    val cycles: Int,
    val capacity: Int
)