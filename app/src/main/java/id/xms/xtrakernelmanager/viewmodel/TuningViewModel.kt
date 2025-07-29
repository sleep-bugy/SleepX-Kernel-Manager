package id.xms.xtrakernelmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.data.repository.RootRepository
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TuningViewModel @Inject constructor(
    private val systemRepo: SystemRepository,
    private val rootRepo: RootRepository
) : ViewModel() {

    val clusters = flow { emit(systemRepo.getCpuClusters()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun applyCpuGov(clusterName: String, gov: String) = viewModelScope.launch {
        val path = when (clusterName) {
            "Little" -> "cpu0"
            "Big" -> "cpu4"
            "Prime" -> "cpu7"
            else -> "cpu0"
        }
        rootRepo.run("echo $gov > /sys/devices/system/cpu/$path/cpufreq/scaling_governor")
    }

    fun setThermal(mode: String) = viewModelScope.launch {
        rootRepo.run("echo $mode > /sys/class/thermal/thermal_message/config")
    }
}