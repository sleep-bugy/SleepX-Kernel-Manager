package id.xms.xtrakernelmanager.model

data class UpdateInfo(
    val version: String = "",
    val changelog: String = "",
    val url: String = "",
    val force: Boolean = false
)
