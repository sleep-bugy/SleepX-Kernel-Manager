package id.xms.xtrakernelmanager.data.repository

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class TuningRepository @Inject constructor(
    private val context: Context
) {

    private val TAG = "TuningRepository"

    // Thermal
    private val thermalSysfsNode = "/sys/class/thermal/thermal_message/sconfig"

    // CPU
    private val cpuBaseSysfsPath = "/sys/devices/system/cpu"
    private val coreOnlinePath = "$cpuBaseSysfsPath/cpu%d/online"
    private val cpuGovPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_governor"
    private val cpuMinFreqPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_min_freq"
    private val cpuMaxFreqPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_max_freq"
    private val cpuAvailableGovsPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_available_governors"
    private val cpuAvailableFreqsPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_available_frequencies"

    // GPU
    private val gpuBaseSysfsPath = "/sys/class/kgsl/kgsl-3d0"
    private val gpuGovPath = "$gpuBaseSysfsPath/devfreq/governor"
    private val gpuAvailableGovsPath = "$gpuBaseSysfsPath/devfreq/available_governors"
    private val gpuMinFreqPath = "$gpuBaseSysfsPath/devfreq/min_freq"
    private val gpuMaxFreqPath = "$gpuBaseSysfsPath/devfreq/max_freq"
    private val gpuAvailableFreqsPath = "$gpuBaseSysfsPath/devfreq/available_frequencies"
    private val gpuCurrentPowerLevelPath = "$gpuBaseSysfsPath/default_pwrlevel"
    private val gpuMinPowerLevelPath = "$gpuBaseSysfsPath/min_pwrlevel"
    private val gpuMaxPowerLevelPath = "$gpuBaseSysfsPath/max_pwrlevel"

    // RAM
    private val zramControlPath = "/sys/block/zram0"
    private val zramResetPath = "$zramControlPath/reset"
    private val zramDisksizePath = "$zramControlPath/disksize"
    private val zramCompAlgorithmPath = "$zramControlPath/comp_algorithm"
    private val zramInitStatePath = "$zramControlPath/initstate"
    private val swappinessPath = "/proc/sys/vm/swappiness"
    private val dirtyRatioPath = "/proc/sys/vm/dirty_ratio"
    private val dirtyBackgroundRatioPath = "/proc/sys/vm/dirty_background_ratio"
    private val dirtyWritebackCentisecsPath = "/proc/sys/vm/dirty_writeback_centisecs"
    private val dirtyExpireCentisecsPath = "/proc/sys/vm/dirty_expire_centisecs"
    private val minFreeKbytesPath = "/proc/sys/vm/min_free_kbytes"

    /* ----------------------------------------------------------
       Helper Shell
       ---------------------------------------------------------- */
    private fun runShellCommand(cmd: String): Boolean {
        Log.d(TAG, "[Shell RW] Executing: '$cmd'")
        val result = Shell.cmd(cmd).exec()
        return if (result.isSuccess) {
            Log.i(TAG, "[Shell RW] Success (code ${result.code}): '$cmd'")
            true
        } else {
            Log.e(TAG, "[Shell RW] Failed (code ${result.code}): '$cmd'. Err: ${result.err.joinToString("\n")}")
            false
        }
    }

    private fun readShellCommand(cmd: String): String {
        Log.d(TAG, "[Shell R] Executing: '$cmd'")
        val result = Shell.cmd(cmd).exec()
        return if (result.isSuccess) result.out.joinToString("\n").trim() else ""
    }

    /* ----------------------------------------------------------
       RAM detection
       ---------------------------------------------------------- */
    private val totalRamBytes: Long
        get() {
            val memInfo = ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getMemoryInfo(memInfo)
            return memInfo.totalMem
        }

    /* ----------------------------------------------------------
   ZRAM – resize tanpa reboot
   ---------------------------------------------------------- */

    private fun ensureZramNode() {
        // kalau node belum ada, lewati saja – ROM kamu sudah 4 GB built-in
        // fungsi ini hanya jaga-jaga
    }

    fun calculateMaxZramSize(): Long {
        // Gunakan pembulatan ke atas untuk mendapatkan GB yang lebih akurat
        // Contoh: 7.8GB akan dibulatkan menjadi 8GB, bukan 7GB
        val ramGB = ceil(totalRamBytes / (1024.0 * 1024.0 * 1024.0)).toLong()
        return when {
            ramGB <= 3L -> 1_073_741_824L   // 1 GB (untuk RAM <= 3GB)
            ramGB <= 4L -> 2_147_483_648L   // 2 GB (untuk RAM > 3GB dan <= 4GB)
            ramGB <= 6L -> 4_294_967_296L   // 4 GB (untuk RAM > 4GB dan <= 6GB)
            ramGB <= 8L -> 9_663_676_416L   // 9 GB (untuk RAM > 6GB dan <= 8GB)
            ramGB <= 12L -> 15_032_385_536L // 14 GB (untuk RAM > 8GB dan <= 12GB)
            ramGB <= 16L -> 19_327_352_832L // 18 GB (untuk RAM > 12GB dan <= 16GB)
            else -> totalRamBytes / 4       // fallback 25 %
        }
    }

    fun resizeZramSafely(newSizeBytes: Long): Boolean =
        runShellCommand(
            """
        swapoff /dev/block/zram0 2>/dev/null || true
        echo 1 > /sys/block/zram0/reset
        echo $newSizeBytes > /sys/block/zram0/disksize
        mkswap /dev/block/zram0 2>/dev/null || true
        swapon /dev/block/zram0 2>/dev/null || true
        """.trimIndent()
        )


    fun setZramEnabled(enabled: Boolean): Flow<Boolean> = flow {
        if (enabled) resizeZramSafely(calculateMaxZramSize()) else resizeZramSafely(0)
        emit(readShellCommand("cat $zramInitStatePath").trim() == "1")
    }.flowOn(Dispatchers.IO)

    fun setZramDisksize(sizeBytes: Long): Boolean =
        resizeZramSafely(sizeBytes)

    fun getZramDisksize(): Flow<Long> = flow {
        emit(readShellCommand("cat $zramDisksizePath").toLongOrNull() ?: 0L)
    }.flowOn(Dispatchers.IO)

    fun getZramEnabled(): Flow<Boolean> = flow {
        emit(readShellCommand("cat $zramInitStatePath").trim() == "1")
    }.flowOn(Dispatchers.IO)

    fun setCompressionAlgorithm(algo: String): Boolean {
        val currentSize = readShellCommand("cat $zramDisksizePath").toLongOrNull() ?: 0L
        // Ensure ZRAM node exists and is initialized (even if size is 0)
        if (!Shell.cmd("test -e $zramControlPath").exec().isSuccess) {
            Log.w(TAG, "ZRAM node $zramControlPath does not exist. Cannot set compression algorithm.")
            return false
        }

        // If zram is active, we need to resize it to 0, set algorithm, then resize back
        val commands = if (currentSize > 0) {
            """
            swapoff /dev/block/zram0 2>/dev/null || true
            echo 1 > $zramResetPath
            echo $algo > $zramCompAlgorithmPath
            echo $currentSize > $zramDisksizePath
            mkswap /dev/block/zram0 2>/dev/null || true
            swapon /dev/block/zram0 2>/dev/null || true
            """.trimIndent()
        } else {
            // If zram is not active or size is 0, just set the algorithm
            "echo $algo > $zramCompAlgorithmPath"
        }
        return runShellCommand(commands)
    }

    fun getCompressionAlgorithms(): Flow<List<String>> = flow {
        emit(readShellCommand("cat $zramCompAlgorithmPath").split(" ").filter { it.isNotBlank() }.sorted())
    }.flowOn(Dispatchers.IO)

    fun getCurrentCompression(): Flow<String> = flow {
        val raw = readShellCommand("cat $zramCompAlgorithmPath").trim()
        val active = raw.split(" ").firstOrNull { it.startsWith("[") } // yang ada tanda []
            ?.removeSurrounding("[", "]")
            ?: raw.split(" ").firstOrNull()
            ?: "lz4"
        emit(active)
    }.flowOn(Dispatchers.IO)

    fun setSwappiness(value: Int): Boolean =
        runShellCommand("echo $value > /proc/sys/vm/swappiness")

    fun getSwappiness(): Flow<Int> = flow {
        emit(readShellCommand("cat /proc/sys/vm/swappiness").toIntOrNull() ?: 60)
    }.flowOn(Dispatchers.IO)

    fun setDirtyRatio(value: Int): Boolean =
        runShellCommand("echo $value > $dirtyRatioPath")

    fun getDirtyRatio(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyRatioPath").toIntOrNull() ?: 20)
    }.flowOn(Dispatchers.IO)

    fun setDirtyBackgroundRatio(value: Int): Boolean =
        runShellCommand("echo $value > $dirtyBackgroundRatioPath")

    fun getDirtyBackgroundRatio(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyBackgroundRatioPath").toIntOrNull() ?: 10)
    }.flowOn(Dispatchers.IO)

    fun setDirtyWriteback(value: Int): Boolean =
        runShellCommand("echo ${value * 100} > $dirtyWritebackCentisecsPath")

    fun getDirtyWriteback(): Flow<Int> = flow {
        emit((readShellCommand("cat $dirtyWritebackCentisecsPath").toIntOrNull() ?: 3000) / 100)
    }.flowOn(Dispatchers.IO)

    fun setDirtyExpireCentisecs(value: Int): Boolean =
        runShellCommand("echo $value > $dirtyExpireCentisecsPath")

    fun getDirtyExpireCentisecs(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyExpireCentisecsPath").toIntOrNull() ?: 300)
    }.flowOn(Dispatchers.IO)

    fun setMinFreeMemory(value: Int): Boolean =
        runShellCommand("echo ${value * 1024} > $minFreeKbytesPath")

    fun getMinFreeMemory(): Flow<Int> = flow {
        emit((readShellCommand("cat $minFreeKbytesPath").toIntOrNull() ?: 131072) / 1024)
    }.flowOn(Dispatchers.IO)

    /* ----------------------------------------------------------
       CPU
       ---------------------------------------------------------- */
    fun getCpuGov(cluster: String): Flow<String> = flow {
        emit(readShellCommand("cat ${cpuGovPath.format(cluster)}"))
    }.flowOn(Dispatchers.IO)

    fun setCpuGov(cluster: String, gov: String): Boolean =
        runShellCommand("echo $gov > ${cpuGovPath.format(cluster)}")

    fun getCpuFreq(cluster: String): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat ${cpuMinFreqPath.format(cluster)}").toIntOrNull() ?: 0
        val max = readShellCommand("cat ${cpuMaxFreqPath.format(cluster)}").toIntOrNull() ?: 0
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun setCpuFreq(cluster: String, min: Int, max: Int): Boolean =
        runShellCommand("echo $min > ${cpuMinFreqPath.format(cluster)}") &&
                runShellCommand("echo $max > ${cpuMaxFreqPath.format(cluster)}")

    fun getAvailableCpuGovernors(cluster: String): Flow<List<String>> = flow {
        emit(readShellCommand("cat ${cpuAvailableGovsPath.format(cluster)}")
            .split(" ")
            .filter { it.isNotBlank() }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getAvailableCpuFrequencies(cluster: String): Flow<List<Int>> = flow {
        emit(readShellCommand("cat ${cpuAvailableFreqsPath.format(cluster)}")
            .split(" ")
            .mapNotNull { it.toIntOrNull() }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun setCoreOnline(coreId: Int, isOnline: Boolean): Boolean =
        runShellCommand("echo ${if (isOnline) 1 else 0} > ${coreOnlinePath.format(coreId)}")

    fun getCoreOnline(coreId: Int): Boolean =
        readShellCommand("cat ${coreOnlinePath.format(coreId)}").trim() == "1"

    /* ----------------------------------------------------------
       GPU
       ---------------------------------------------------------- */
    fun getGpuGov(): Flow<String> = flow {
        emit(readShellCommand("cat $gpuGovPath"))
    }.flowOn(Dispatchers.IO)

    fun setGpuGov(gov: String): Boolean =
        runShellCommand("echo $gov > $gpuGovPath")

    fun getGpuFreq(): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat $gpuMinFreqPath").toIntOrNull() ?: 0
        val max = readShellCommand("cat $gpuMaxFreqPath").toIntOrNull() ?: 0
        emit((min / 1000) to (max / 1000))
    }.flowOn(Dispatchers.IO)

    fun setGpuMinFreq(freqKHz: Int): Boolean =
        runShellCommand("echo ${freqKHz * 1000} > $gpuMinFreqPath")

    fun setGpuMaxFreq(freqKHz: Int): Boolean =
        runShellCommand("echo ${freqKHz * 1000} > $gpuMaxFreqPath")

    fun getAvailableGpuGovernors(): Flow<List<String>> = flow {
        emit(readShellCommand("cat $gpuAvailableGovsPath")
            .split(" ")
            .filter { it.isNotBlank() }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getAvailableGpuFrequencies(): Flow<List<Int>> = flow {
        emit(readShellCommand("cat $gpuAvailableFreqsPath")
            .split(" ")
            .mapNotNull { it.toIntOrNull() }
            .map { it / 1000 }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getGpuPowerLevelRange(): Flow<Pair<Float, Float>> = flow {
        val min = readShellCommand("cat $gpuMinPowerLevelPath").toFloatOrNull() ?: 0f
        val max = readShellCommand("cat $gpuMaxPowerLevelPath").toFloatOrNull() ?: 0f
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun getCurrentGpuPowerLevel(): Flow<Float> = flow {
        emit(readShellCommand("cat $gpuCurrentPowerLevelPath").toFloatOrNull() ?: 0f)
    }.flowOn(Dispatchers.IO)

    fun setGpuPowerLevel(level: Float): Boolean =
        runShellCommand("echo ${level.toInt()} > $gpuCurrentPowerLevelPath")

    /* ----------------------------------------------------------
       Thermal
       ---------------------------------------------------------- */
    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        emit(readShellCommand("cat $thermalSysfsNode").toIntOrNull() ?: -1)
    }.flowOn(Dispatchers.IO)

    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        val ok = runShellCommand("chmod 0777 $thermalSysfsNode") &&
                runShellCommand("echo $modeIndex > $thermalSysfsNode") &&
                runShellCommand("chmod 0644 $thermalSysfsNode")
        emit(ok)
    }.flowOn(Dispatchers.IO)

    /* ----------------------------------------------------------
       OpenGL / Vulkan / Renderer
       ---------------------------------------------------------- */
    fun getOpenGlesDriver(): Flow<String> = flow {
        val out = Shell.cmd("dumpsys SurfaceFlinger | grep \"GLES:\"").exec().out.joinToString("\n").trim()
        emit(out.substringAfter("GLES:").trim().ifEmpty { "N/A" })
    }.flowOn(Dispatchers.IO)

    fun getGpuRenderer(): Flow<String> = flow {
        emit(readShellCommand("getprop debug.hwui.renderer").ifEmpty { "OpenGL" })
    }.flowOn(Dispatchers.IO)

    fun setGpuRenderer(renderer: String): Flow<Boolean> = flow {
        val ok = runShellCommand("setprop debug.hwui.renderer $renderer")
        emit(ok)
    }.flowOn(Dispatchers.IO)

    fun getVulkanApiVersion(): Flow<String> = flow {
        emit(readShellCommand("getprop ro.hardware.vulkan.version").ifEmpty { "N/A" })
    }.flowOn(Dispatchers.IO)

    fun rebootDevice(): Flow<Boolean> = flow {
        emit(runShellCommand("reboot"))
    }.flowOn(Dispatchers.IO)
}