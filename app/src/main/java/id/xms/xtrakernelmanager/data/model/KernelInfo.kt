package id.xms.xtrakernelmanager.data.model
import kotlinx.serialization.Serializable

@Serializable
data class KernelInfo(
    val version: String,
    val gkiType: String,
    val scheduler: String
)