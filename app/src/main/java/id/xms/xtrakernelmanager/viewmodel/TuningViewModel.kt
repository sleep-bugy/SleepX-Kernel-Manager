package id.xms.xtrakernelmanager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import id.xms.xtrakernelmanager.data.repository.RootRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TuningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemRepo: SystemRepository,
    private val rootRepo: RootRepository
) : ViewModel() {

    val clusters = systemRepo.getCpuClusters()
        .let { it } // sudah list, tidak perlu flow

    fun applyCpuGov(clusterName: String, gov: String) = viewModelScope.launch {
        val path = when (clusterName) {
            "Little" -> "cpu0"
            "Big"    -> "cpu4"
            "Prime"  -> "cpu7"
            else     -> "cpu0"
        }
        rootRepo.run("echo $gov > /sys/devices/system/cpu/$path/cpufreq/scaling_governor")
    }

    fun setThermal(mode: String) = viewModelScope.launch {
        rootRepo.run("echo $mode > /sys/class/thermal/thermal_message/config")
    }
}