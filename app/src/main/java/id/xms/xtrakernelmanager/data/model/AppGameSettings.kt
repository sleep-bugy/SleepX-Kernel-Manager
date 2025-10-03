package id.xms.xtrakernelmanager.data.model
data class AppGameSettings(
    val packageName: String,
    var enabled: Boolean = false,
    var defaultPerformanceMode: String = "default",
    var dndEnabled: Boolean = false,
    var clearBackgroundOnLaunch: Boolean = false,
    var showFpsCounter: Boolean = true,
    var showFullOverlay: Boolean = false
)
