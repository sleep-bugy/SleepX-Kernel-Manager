package id.xms.xtrakernelmanager.data.model
import kotlinx.serialization.Serializable

@Serializable
data class GpuInfo(
    val renderer: String,
    val glEsVersion: String,
    val vulkanVersion: String,
    val governor: String,
    val availableGovernors: List<String>,
    val minFreq: Int,
    val maxFreq: Int
)