package id.xms.xtrakernelmanager.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.data.repository.TuningRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PerformanceTileService : TileService() {

    @Inject
    lateinit var tuningRepository: TuningRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Apply performance governor to all CPU clusters
        serviceScope.launch {
            cpuClusters.forEach { cluster ->
                tuningRepository.setCpuGov(cluster, "performance")
            }

            // Update tile state
            updateTileState()
        }
    }

    private fun updateTileState() {
        qsTile?.apply {
            label = "Performance"
            contentDescription = "Activate Performance mode with performance governor"
            state = Tile.STATE_INACTIVE
            icon = android.graphics.drawable.Icon.createWithResource(
                applicationContext,
                android.R.drawable.ic_media_ff
            )
            updateTile()
        }
    }
}
