package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.model.AppGameSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameControlRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("game_control_settings", Context.MODE_PRIVATE)
    suspend fun getAppSettings(packageName: String): AppGameSettings? = withContext(Dispatchers.IO) {
        if (packageName.isEmpty() || !sharedPrefs.contains("$packageName.enabled")) {
            return@withContext null
        }

        return@withContext AppGameSettings(
            packageName = packageName,
            enabled = sharedPrefs.getBoolean("$packageName.enabled", false),
            defaultPerformanceMode = sharedPrefs.getString("$packageName.performanceMode", "default") ?: "default",
            dndEnabled = sharedPrefs.getBoolean("$packageName.dndEnabled", false),
            clearBackgroundOnLaunch = sharedPrefs.getBoolean("$packageName.clearBackground", false),
            showFpsCounter = sharedPrefs.getBoolean("$packageName.showFps", true),
            showFullOverlay = sharedPrefs.getBoolean("$packageName.showFullOverlay", false)
        )
    }
    suspend fun saveAppSettings(settings: AppGameSettings) = withContext(Dispatchers.IO) {
        sharedPrefs.edit().apply {
            putBoolean("${settings.packageName}.enabled", settings.enabled)
            putString("${settings.packageName}.performanceMode", settings.defaultPerformanceMode)
            putBoolean("${settings.packageName}.dndEnabled", settings.dndEnabled)
            putBoolean("${settings.packageName}.clearBackground", settings.clearBackgroundOnLaunch)
            putBoolean("${settings.packageName}.showFps", settings.showFpsCounter)
            putBoolean("${settings.packageName}.showFullOverlay", settings.showFullOverlay)
            apply()
        }
    }

    suspend fun getAllEnabledApps(): List<AppGameSettings> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppGameSettings>()
        val allPrefs = sharedPrefs.all

        // Group preferences by package name
        val packagePrefs = mutableMapOf<String, MutableMap<String, Any>>()

        allPrefs.forEach { (key, value) ->
            val parts = key.split(".")
            if (parts.size >= 2) {
                val packageName = parts[0]
                val setting = parts[1]
                if (value != null) {
                    val settingsMap = packagePrefs.getOrPut(packageName) { mutableMapOf() }
                    settingsMap[setting] = value
                }
            }
        }

        packagePrefs.forEach { (packageName, settings) ->
            val enabled = settings["enabled"] as? Boolean ?: false
            if (enabled) {
                result.add(
                    AppGameSettings(
                        packageName = packageName,
                        enabled = enabled,
                        defaultPerformanceMode = settings["performanceMode"] as? String ?: "default",
                        dndEnabled = settings["dndEnabled"] as? Boolean ?: false,
                        clearBackgroundOnLaunch = settings["clearBackground"] as? Boolean ?: false,
                        showFpsCounter = settings["showFps"] as? Boolean ?: true,
                        showFullOverlay = settings["showFullOverlay"] as? Boolean ?: false
                    )
                )
            }
        }

        return@withContext result
    }
}
