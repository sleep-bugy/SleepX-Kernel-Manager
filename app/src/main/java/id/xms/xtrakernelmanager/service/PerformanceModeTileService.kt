package id.xms.xtrakernelmanager.service

import android.content.Context
import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.repository.TuningRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PerformanceModeTileService : TileService() {

    @Inject
    lateinit var tuningRepository: TuningRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREF_NAME = "performance_mode_tile"
        private const val KEY_CURRENT_MODE = "current_mode"

        // Performance modes
        const val MODE_BATTERY_SAVER = 0
        const val MODE_BALANCED = 1
        const val MODE_PERFORMANCE = 2

        // Governor mappings
        private val GOVERNOR_MAP = mapOf(
            MODE_BATTERY_SAVER to "powersave",
            MODE_BALANCED to "schedutil",
            MODE_PERFORMANCE to "performance"
        )

        // Mode labels
        private val MODE_LABELS = mapOf(
            MODE_BATTERY_SAVER to "Battery Saver",
            MODE_BALANCED to "Balanced",
            MODE_PERFORMANCE to "Performance"
        )

        // Mode descriptions
        private val MODE_DESCRIPTIONS = mapOf(
            MODE_BATTERY_SAVER to "Power saving mode active",
            MODE_BALANCED to "Balanced performance mode",
            MODE_PERFORMANCE to "Maximum performance mode"
        )
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Get current mode and cycle to next
        val currentMode = prefs.getInt(KEY_CURRENT_MODE, MODE_BALANCED)
        val nextMode = when (currentMode) {
            MODE_BATTERY_SAVER -> MODE_BALANCED
            MODE_BALANCED -> MODE_PERFORMANCE
            MODE_PERFORMANCE -> MODE_BATTERY_SAVER
            else -> MODE_BALANCED
        }

        // Save new mode
        prefs.edit().putInt(KEY_CURRENT_MODE, nextMode).apply()

        // Apply the corresponding governor
        val governor = GOVERNOR_MAP[nextMode] ?: "schedutil"
        serviceScope.launch {
            cpuClusters.forEach { cluster ->
                tuningRepository.setCpuGov(cluster, governor)
            }
        }

        // Update tile state
        updateTileState()
    }

    private fun updateTileState() {
        val currentMode = prefs.getInt(KEY_CURRENT_MODE, MODE_BALANCED)

        qsTile?.apply {
            label = MODE_LABELS[currentMode] ?: "Balanced"
            contentDescription = MODE_DESCRIPTIONS[currentMode] ?: "Performance mode"

            // Set different states and icons based on mode
            when (currentMode) {
                MODE_BATTERY_SAVER -> {
                    state = Tile.STATE_ACTIVE
                    icon = android.graphics.drawable.Icon.createWithResource(
                        applicationContext,
                        R.drawable.ic_battery_saver
                    )
                }
                MODE_BALANCED -> {
                    state = Tile.STATE_INACTIVE
                    icon = android.graphics.drawable.Icon.createWithResource(
                        applicationContext,
                        R.drawable.ic_balance
                    )
                }
                MODE_PERFORMANCE -> {
                    state = Tile.STATE_ACTIVE
                    icon = android.graphics.drawable.Icon.createWithResource(
                        applicationContext,
                        R.drawable.ic_performance
                    )
                }
            }

            updateTile()
        }
    }
}
