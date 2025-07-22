package id.xms.xtrakernelmanager.viewmodel

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.xms.xtrakernelmanager.util.EasterEggUtils
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class HomeUiState(
    val isRooted: Boolean = false,
    val batteryLevel: Float = 0f,
    val cpuCores: Int = 0,
    val maxFreq: String = "N/A",
    val minFreq: String = "N/A",
    val kernelVersion: String = "N/A",
    val usedRam: String = "N/A",
    val showEasterEgg: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        updateSystemInfo(context)
    }

    private fun updateSystemInfo(context: Context) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRooted = RootUtils.isDeviceRooted(),
                    batteryLevel = getBatteryLevel(context),
                    cpuCores = getCpuCoreCount(),
                    maxFreq = getPrimeClusterMaxFreq() ?: "N/A",
                    minFreq = getCpuFreq("min") ?: "N/A",
                    kernelVersion = getKernelVersion(),
                    usedRam = getUsedRam()
                )
            }
        }
    }

    private fun getBatteryLevel(context: Context): Float {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
    }

    private fun getCpuCoreCount(): Int {
        return try {
            val cpuFiles = object {}.javaClass.classLoader?.getResourceAsStream("/sys/devices/system/cpu/")?.let {
                it.bufferedReader().use { reader ->
                    reader.readLines().filter { it.contains("cpu[0-9]+") }.size
                }
            } ?: 0
            if (cpuFiles == 0) Runtime.getRuntime().availableProcessors() else cpuFiles
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error getting CPU core count: ${e.message}")
            0
        }
    }

    private fun getPrimeClusterMaxFreq(): String? {
        return if (RootUtils.isDeviceRooted()) {
            try {
                val process = Runtime.getRuntime().exec("su")
                val output = process.inputStream.bufferedReader()
                process.outputStream.use { it.write("for cpu in /sys/devices/system/cpu/cpu[0-9]*; do cat \$cpu/cpufreq/cpuinfo_max_freq; done | sort -nr | head -n 1\n".toByteArray()) }
                process.outputStream.flush()
                val result = output.readLine()?.toIntOrNull()?.div(1000)
                result?.toString()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting max freq: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    private fun getCpuFreq(type: String): String? {
        return if (RootUtils.isDeviceRooted()) {
            try {
                val process = Runtime.getRuntime().exec("su")
                val output = process.inputStream.bufferedReader()
                val command = when (type) {
                    "min" -> "cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"
                    else -> return null
                }
                process.outputStream.use { it.write("$command\n".toByteArray()) }
                process.outputStream.flush()
                val result = output.readLine()?.toIntOrNull()?.div(1000)
                result?.toString()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting $type freq: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    private fun getKernelVersion(): String {
        return if (RootUtils.isDeviceRooted()) {
            try {
                val process = Runtime.getRuntime().exec("su")
                val output = process.inputStream.bufferedReader()
                process.outputStream.use { it.write("cat /proc/version\n".toByteArray()) }
                process.outputStream.flush()
                output.readLine()?.split(" ")?.getOrNull(2) ?: "N/A"
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting kernel version: ${e.message}")
                "N/A"
            }
        } else {
            "N/A"
        }
    }

    private fun getUsedRam(): String {
        return if (RootUtils.isDeviceRooted()) {
            try {
                val process = Runtime.getRuntime().exec("su")
                val output = process.inputStream.bufferedReader()
                process.outputStream.use { it.write("cat /proc/meminfo\n".toByteArray()) }
                process.outputStream.flush()
                var totalRam = 0L
                var availableRam = 0L
                output.useLines { lines ->
                    lines.forEach { line ->
                        val pattern = Pattern.compile("(\\w+):\\s+(\\d+)\\s+kB")
                        val matcher = pattern.matcher(line.trim())
                        if (matcher.matches()) {
                            val value = matcher.group(2)?.toLongOrNull() ?: 0L
                            when (matcher.group(1)) {
                                "MemTotal" -> totalRam = value
                                "MemAvailable" -> availableRam = value
                            }
                        } else {
                            Log.w("HomeViewModel", "Unmatched line: $line")
                        }
                    }
                }
                if (totalRam > 0 && availableRam > 0) {
                    "${(totalRam - availableRam) / 1024} MB"
                } else {
                    Log.e("HomeViewModel", "Failed to calculate used RAM: total=$totalRam, available=$availableRam")
                    "N/A"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error reading /proc/meminfo: ${e.message}")
                "N/A"
            }
        } else {
            "N/A"
        }
    }

    fun onRootStatusClick() {
        if (EasterEggUtils.onClick()) {
            _uiState.update { it.copy(showEasterEgg = true) }
            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(showEasterEgg = false) }
                EasterEggUtils.resetClickCount()
            }
        }
    }
}