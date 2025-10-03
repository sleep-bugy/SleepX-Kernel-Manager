package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpuFpsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _currentFps = MutableStateFlow(0)
    val currentFps: Flow<Int> = _currentFps.asStateFlow()

    private val _cpuLoad = MutableStateFlow(0)
    val cpuLoad: Flow<Int> = _cpuLoad.asStateFlow()

    private val _gpuLoad = MutableStateFlow(0)
    val gpuLoad: Flow<Int> = _gpuLoad.asStateFlow()

    private val _cpuGovernor = MutableStateFlow("")
    val cpuGovernor: Flow<String> = _cpuGovernor.asStateFlow()

    private val _gpuGovernor = MutableStateFlow("")
    val gpuGovernor: Flow<String> = _gpuGovernor.asStateFlow()

    /**
     * Get current FPS from DRM or AOSP logs
     */
    suspend fun getFps(): Int = withContext(Dispatchers.IO) {
        // Try to get from DRM first
        val drmRaw = RootUtils.runCommandAsRoot("cat /sys/class/drm/sde-crtc-0/measured_fps") ?: ""
        if (drmRaw.contains("fps:")) {
            val fps = drmRaw
                .substringAfter("fps:")
                .substringBefore("duration")
                .trim()
                .toFloatOrNull()?.toInt() ?: 0

            if (fps > 0) {
                _currentFps.value = fps
                return@withContext fps
            }
        }

        // Fallback to AOSP FPS log
        val log = RootUtils.runCommandAsRoot("logcat -d -s SurfaceFlinger | tail -40") ?: ""
        val aospFps = Regex("""fps\s+(\d+\.?\d*)""").findAll(log)
            .mapNotNull { it.groupValues[1].toFloatOrNull() }
            .lastOrNull()?.toInt() ?: 60

        _currentFps.value = aospFps
        return@withContext aospFps
    }

    /**
     * Get CPU load as percentage
     */
    suspend fun getCpuLoad(): Int = withContext(Dispatchers.IO) {
        val result = RootUtils.runCommandAsRoot("cat /proc/stat | grep 'cpu '") ?: return@withContext 0

        // Parse CPU utilization
        val parts = result.split(" ").filter { it.isNotEmpty() }
        if (parts.size < 8) return@withContext 0

        val total = parts.subList(1, parts.size).sumOf { it.toLongOrNull() ?: 0 }
        val idle = parts[4].toLongOrNull() ?: 0

        val cpuUsage = 100 * (1 - idle.toFloat() / total)
        val usage = cpuUsage.toInt().coerceIn(0, 100)

        _cpuLoad.value = usage
        return@withContext usage
    }

    /**
     * Get GPU load as percentage
     */
    suspend fun getGpuLoad(): Int = withContext(Dispatchers.IO) {
        // Different devices store GPU info in different locations
        var gpuLoad = 0

        // Try Adreno
        val adrenoResult = RootUtils.runCommandAsRoot("cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage") ?: ""
        if (adrenoResult.trim().matches(Regex("\\d+"))) {
            gpuLoad = adrenoResult.trim().toIntOrNull() ?: 0
            _gpuLoad.value = gpuLoad
            return@withContext gpuLoad
        }

        // Try Mali
        val maliResult = RootUtils.runCommandAsRoot("cat /sys/devices/platform/mali.0/utilization") ?: ""
        if (maliResult.contains("=")) {
            gpuLoad = maliResult.substringAfter("=").trim().toIntOrNull() ?: 0
            _gpuLoad.value = gpuLoad
            return@withContext gpuLoad
        }

        _gpuLoad.value = 0
        return@withContext 0
    }

    /**
     * Get CPU governor
     */
    suspend fun getCpuGovernor(): String = withContext(Dispatchers.IO) {
        val result = RootUtils.runCommandAsRoot("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor") ?: ""
        val governor = result.trim()
        _cpuGovernor.value = governor
        return@withContext governor
    }

    /**
     * Get GPU governor
     */
    suspend fun getGpuGovernor(): String = withContext(Dispatchers.IO) {
        // Try Adreno
        val adrenoResult = RootUtils.runCommandAsRoot("cat /sys/class/kgsl/kgsl-3d0/devfreq/governor") ?: ""
        if (adrenoResult.isNotEmpty()) {
            val governor = adrenoResult.trim()
            _gpuGovernor.value = governor
            return@withContext governor
        }

        // Try Mali
        val maliResult = RootUtils.runCommandAsRoot("cat /sys/devices/platform/mali.0/power_policy") ?: ""
        if (maliResult.isNotEmpty()) {
            val governor = maliResult.trim()
            _gpuGovernor.value = governor
            return@withContext governor
        }

        _gpuGovernor.value = "unknown"
        return@withContext "unknown"
    }
}
