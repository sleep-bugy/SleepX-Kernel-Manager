package id.xms.xtrakernelmanager.data.model
import kotlinx.serialization.Serializable

@Serializable
data class SystemInfo(
    val model: String,
    val codename: String,
    val androidVersion: String,
    val sdk: Int,
    val soc: String,
    val fingerprint: String,
    // Display Information
    val screenResolution: String,
    val displayTechnology: String,
    val refreshRate: String,
    val screenDpi: String,
    val gpuRenderer: String
)