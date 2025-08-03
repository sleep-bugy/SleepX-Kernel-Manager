package id.xms.xtrakernelmanager.data.repository

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TuningRepository @Inject constructor() {

    private val thermalSysfsNode = "/sys/class/thermal/thermal_message/sconfig"
    private val defaultThermalNodePermissions = "0644"
    private val temporaryWritablePermissions = "0777"

    private val cpuBaseSysfsPath = "/sys/devices/system/cpu"

    private val gpuBaseSysfsPath = "/sys/class/kgsl/kgsl-3d0"
    private val gpuGovPath = "$gpuBaseSysfsPath/devfreq/governor"
    private val gpuAvailableGovsPath = "$gpuBaseSysfsPath/devfreq/available_governors"
    private val gpuMinFreqPath = "$gpuBaseSysfsPath/devfreq/min_freq"
    private val gpuMaxFreqPath = "$gpuBaseSysfsPath/devfreq/max_freq"
    private val gpuAvailableFreqsPath = "$gpuBaseSysfsPath/devfreq/available_frequencies"

    private val gpuCurrentPowerLevelPath = "$gpuBaseSysfsPath/default_pwrlevel"
    private val gpuMinPowerLevelPath = "$gpuBaseSysfsPath/min_pwrlevel"
    private val gpuMaxPowerLevelPath = "$gpuBaseSysfsPath/max_pwrlevel"
    private val defaultPowerLevelNodePermissions = "0644"
    private val temporaryWritablePowerLevelPermissions = "0777"

    private val swappinessPath = "/proc/sys/vm/swappiness"



    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        val commands = listOf(
            "chmod $temporaryWritablePermissions $thermalSysfsNode",
            "echo $modeIndex > $thermalSysfsNode",
            "chmod $defaultThermalNodePermissions $thermalSysfsNode"
        )
        Log.d("TuningRepo_Thermal", "Set thermal index to $modeIndex")
        try {
            Shell.cmd(commands[0]).exec().let {
                if (!it.isSuccess) {
                    Log.e("TuningRepo_Thermal", "Chmod writable failed: ${it.err.joinToString()}")
                    emit(false); return@flow
                }
            }
            val echoResult = Shell.cmd(commands[1]).exec()
            var success = false
            if (echoResult.isSuccess) {
                val verifyResult = Shell.cmd("cat $thermalSysfsNode").exec()
                if (verifyResult.isSuccess && verifyResult.out.firstOrNull()?.trim() == modeIndex.toString()) {
                    success = true
                } else {
                    Log.w("TuningRepo_Thermal", "Verification failed. Current: ${verifyResult.out.firstOrNull()}, Expected: $modeIndex")
                }
            } else {
                Log.e("TuningRepo_Thermal", "Echo failed: ${echoResult.err.joinToString()}")
            }
            Shell.cmd(commands[2]).exec().let {
                if (!it.isSuccess) Log.w("TuningRepo_Thermal", "Chmod restore failed: ${it.err.joinToString()}")
            }
            emit(success)
        } catch (e: Exception) {
            Log.e("TuningRepo_Thermal", "Exception: ${e.message}", e); emit(false)
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        Log.d("TuningRepo_Thermal", "Get current thermal index")
        try {
            val result = Shell.cmd("cat $thermalSysfsNode").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.first().trim().toIntOrNull()?.let { emit(it) } ?: emit(-1)
            } else {
                Log.e("TuningRepo_Thermal", "Read failed. Code: ${result.code}, Err: ${result.err.joinToString()}")
                emit(-1)
            }
        } catch (e: Exception) {
            Log.e("TuningRepo_Thermal", "Exception: ${e.message}", e); emit(-1)
        }
    }.flowOn(Dispatchers.IO)


    /* ---------- CPU Control ---------- */

    fun setCpuGov(cluster: String, gov: String) {
        runShellCommand("echo $gov > $cpuBaseSysfsPath/$cluster/cpufreq/scaling_governor")
    }

    fun setCpuFreq(cluster: String, min: Int, max: Int) {
        runShellCommand("echo $min > $cpuBaseSysfsPath/$cluster/cpufreq/scaling_min_freq")
        runShellCommand("echo $max > $cpuBaseSysfsPath/$cluster/cpufreq/scaling_max_freq")
    }

    fun getCpuGov(cluster: String): Flow<String> = flow {
        emit(readShellCommand("cat $cpuBaseSysfsPath/$cluster/cpufreq/scaling_governor").trim())
    }.flowOn(Dispatchers.IO)

    fun getAvailableCpuGovernors(cluster: String): Flow<List<String>> = flow {
        val path = "$cpuBaseSysfsPath/$cluster/cpufreq/scaling_available_governors"
        try {
            val result = readShellCommand("cat $path").trim()
            if (result.isNotEmpty()) emit(result.split(" ").filter { it.isNotBlank() }.sorted())
            else emit(emptyList())
        } catch (e: Exception) {
            Log.e("TuningRepo_CPU", "Err getAvailableCpuGovernors $cluster: ${e.message}", e); emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getCpuFreq(cluster: String): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat $cpuBaseSysfsPath/$cluster/cpufreq/scaling_min_freq").trim().toIntOrNull() ?: 0
        val max = readShellCommand("cat $cpuBaseSysfsPath/$cluster/cpufreq/scaling_max_freq").trim().toIntOrNull() ?: 0
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun getAvailableCpuFrequencies(cluster: String): Flow<List<Int>> = flow {
        val path = "$cpuBaseSysfsPath/$cluster/cpufreq/scaling_available_frequencies"
        try {
            val result = readShellCommand("cat $path").trim()
            if (result.isNotEmpty()) emit(result.split(" ").mapNotNull { it.toIntOrNull() }.sorted())
            else emit(emptyList())
        } catch (e: Exception) {
            Log.e("TuningRepo_CPU", "Err getAvailableCpuFrequencies $cluster: ${e.message}", e); emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)


    /* ---------- GPU Control ---------- */

    fun setGpuGov(gov: String) {
        runShellCommand("echo $gov > $gpuGovPath")
    }

    @Deprecated("Use setGpuMinFreq and setGpuMaxFreq individually", ReplaceWith("setGpuMinFreq(min); setGpuMaxFreq(max)"))
    fun setGpuFreq(min: Int, max: Int) {
        setGpuMinFreq(min)
        setGpuMaxFreq(max)
    }

    fun setGpuMinFreq(freq: Int) {
        runShellCommand("echo ${freq * 1000} > $gpuMinFreqPath")
    }

    fun setGpuMaxFreq(freq: Int) {
        runShellCommand("echo ${freq * 1000} > $gpuMaxFreqPath")
    }

    fun getGpuGov(): Flow<String> = flow {
        emit(readShellCommand("cat $gpuGovPath").trim())
    }.flowOn(Dispatchers.IO)
    fun getAvailableGpuGovernors(): Flow<List<String>> = flow {
        try {
            val result = readShellCommand("cat $gpuAvailableGovsPath").trim()
            if (result.isNotEmpty()) emit(result.split(" ").filter { it.isNotBlank() }.sorted()) //
            else {
                Log.w("TuningRepo_GPU", "No available GPU governors found or error reading from $gpuAvailableGovsPath.")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("TuningRepo_GPU", "Exception reading available GPU governors: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getGpuFreq(): Flow<Pair<Int, Int>> = flow {
        val minKHz = readShellCommand("cat $gpuMinFreqPath").trim().toIntOrNull() ?: 0
        val maxKHz = readShellCommand("cat $gpuMaxFreqPath").trim().toIntOrNull() ?: 0
        emit((minKHz / 1000) to (maxKHz / 1000))
    }.flowOn(Dispatchers.IO)

    fun getAvailableGpuFrequencies(): Flow<List<Int>> = flow {
        try {
            val command = "cat $gpuAvailableFreqsPath"
            val frequenciesString = readShellCommand(command).trim()
            if (frequenciesString.isNotEmpty()) {
                val frequenciesList = frequenciesString.split(" ")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .map { it / 1000 }
                    .sorted()
                emit(frequenciesList)
            } else {
                Log.w("TuningRepo_GPU", "No available GPU frequencies found or error reading from $gpuAvailableFreqsPath.")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("TuningRepo_GPU", "Exception reading available GPU frequencies: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getGpuPowerLevelRange(): Flow<Pair<Float, Float>> = flow {
            val min = readShellCommand("cat $gpuMinPowerLevelPath").trim().toFloatOrNull() ?: 0f
            val max = readShellCommand("cat $gpuMaxPowerLevelPath").trim().toFloatOrNull() ?: 0f
            if (max > min) emit(min to max) else emit (0f to 0f)


            val levelsString = readShellCommand("cat /sys/class/kgsl/kgsl-3d0/default_pwrlevel").trim()
            if (levelsString.isNotEmpty()) {
                val levels = levelsString.split(" ").mapNotNull { it.toFloatOrNull() }.sorted()
                if (levels.isNotEmpty()) emit(levels.first() to levels.last())
                else emit(0f to 0f)
            } else {
                emit(0f to 0f)
            }
        Log.w("TuningRepo_GPU", "getGpuPowerLevelRange() is using a placeholder. VERIFY AND IMPLEMENT for your kernel.")

        val knownMinLevel = 0f
        val knownMaxLevel = 12f
        if (knownMaxLevel > knownMinLevel) {
            emit(knownMinLevel to knownMaxLevel)
        } else {

            val minPl = readShellCommand("cat $gpuMinPowerLevelPath").trim().toFloatOrNull()
            val maxPl = readShellCommand("cat $gpuMaxPowerLevelPath").trim().toFloatOrNull()
            if (minPl != null && maxPl != null && maxPl > minPl) {
                emit(minPl to maxPl)
            } else {
                Log.w("TuningRepo_GPU", "Cannot determine GPU power level range from sysfs ($gpuMinPowerLevelPath, $gpuMaxPowerLevelPath) or hardcoded values. Emitting 0f to 0f.")
                emit(0f to 0f)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentGpuPowerLevel(): Flow<Float> = flow {
        try {
            val command = "cat $gpuCurrentPowerLevelPath"
            val levelString = readShellCommand(command).trim()
            val level = levelString.toFloatOrNull()
            if (level != null) {
                emit(level)
            } else {
                Log.w("TuningRepo_GPU", "Failed to parse current GPU power level from '$levelString' at $gpuCurrentPowerLevelPath. Emitting 0f.")
                emit(5f)
            }
        } catch (e: Exception) {
            Log.e("TuningRepo_GPU", "Exception reading current GPU power level: ${e.message}", e)
            emit(5f)
        }
    }.flowOn(Dispatchers.IO)

    fun setGpuPowerLevel(level: Float) {
        Log.d("TuningRepo_GPU", "Setting GPU power level to $level (raw float, kernel might need int)")
        val valueToSet = level.toInt()
        val commands = listOf(
            "chmod $temporaryWritablePowerLevelPermissions $gpuCurrentPowerLevelPath",
            "echo $valueToSet > $gpuCurrentPowerLevelPath",
            "chmod $defaultPowerLevelNodePermissions $gpuCurrentPowerLevelPath"
        )

        try {
            Shell.cmd(commands[0]).exec().let {
                if (!it.isSuccess) Log.w("TuningRepo_GPU", "Chmod writable for power level failed: ${it.err.joinToString()}")
            }
            runShellCommand(commands[1])
        } finally {
            Shell.cmd(commands[2]).exec().let {
                if (!it.isSuccess) Log.w("TuningRepo_GPU", "Chmod restore for power level failed: ${it.err.joinToString()}")
            }
        }
    }


    /* ---------- Swappiness ---------- */
    fun setSwappiness(value: Int) {
        runShellCommand("echo $value > $swappinessPath")
    }

    fun getSwappiness(): Flow<Int> = flow {
        try {
            val result = readShellCommand("cat $swappinessPath").trim().toIntOrNull()
            if (result != null) emit(result)
            else { Log.e("TuningRepo_VM", "Failed parse swappiness"); emit(60) }
        } catch (e: Exception) {
            Log.e("TuningRepo_VM", "Exception getSwappiness: ${e.message}", e); emit(60)
        }
    }.flowOn(Dispatchers.IO)


    /* ---------- OpenGLES Driver Info ---------- */
    fun getOpenGlesDriver(): Flow<String> = flow {
        Log.d("TuningRepo_GLES", "Attempting to get OpenGLES driver version using dumpsys SurfaceFlinger")
        try {
            val command = "dumpsys SurfaceFlinger | grep -i 'GLES:'"
            val result = Shell.cmd(command).exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                val fullLine = result.out.firstOrNull()?.trim() ?: ""
                val glesInfo = fullLine.substringAfter("GLES:").trim()
                emit(glesInfo.ifEmpty { "N/A (Info not found in dumpsys)" })
            } else {
                Log.w("TuningRepo_GLES", "dumpsys SurfaceFlinger command failed or returned empty. Code: ${result.code}, Err: ${result.err.joinToString()}")
                val propResult = readShellCommand("getprop ro.opengles.version").trim()
                if (propResult.isNotEmpty()) emit("GLES API Version: $propResult (Driver specific info N/A)")
                emit("N/A (Could not determine or access driver info)")
            }
        } catch (e: Exception) {
            Log.e("TuningRepo_GLES", "Exception getting GLES Driver Version: ${e.message}", e)
            emit("N/A (Error)")
        }
    }.flowOn(Dispatchers.IO)

    /* ---------- GPU Renderer ---------- */
    fun getGpuRenderer(): Flow<String> = flow {
        Log.d("TuningRepo_GPU", "Getting current GPU renderer")
        try {
            val result = readShellCommand("getprop debug.hwui.renderer").trim()
            emit(result.ifEmpty { "OpenGL" })
        } catch (e: Exception) {
            Log.e("TuningRepo_GPU", "Exception getting GPU renderer: ${e.message}", e)
            emit("OpenGL")
        }
    }.flowOn(Dispatchers.IO)

    fun setGpuRenderer(renderer: String): Flow<Boolean> = flow {
        Log.d("TuningRepo_GPU", "Setting GPU renderer to $renderer")
        val validRenderers = listOf("OpenGL", "Vulkan", "Angle", "OpenGL (Skia)", "Vulkan (Skia)")
        if (renderer !in validRenderers) {
            Log.e("TuningRepo_GPU", "Invalid renderer value: $renderer")
            emit(false)
            return@flow
        }
        try {
            runShellCommand("setprop debug.hwui.renderer $renderer")
            emit(true)
        } catch (e: Exception) {
            Log.e("TuningRepo_GPU", "Exception setting GPU renderer: ${e.message}", e)
            emit(false)
        }
    }.flowOn(Dispatchers.IO)

    /* ---------- Vulkan API Version ---------- */
    fun getVulkanApiVersion(): Flow<String> = flow {
        Log.d("TuningRepo_Vulkan", "Attempting to get Vulkan API version using getprop")
        try {
            // Common property for Vulkan API level. OEMs might use different props.
            val command = "getprop ro.hardware.vulkan.version"
            val result = readShellCommand(command).trim()
            if (result.isNotEmpty()) {
                // The property value is usually a hex number (e.g., 0x401000 for Vulkan 1.1.0)
                // For simplicity, we'll return it as is. UI can format it if needed.
                emit("API Level: $result")
            } else {
                emit("N/A (Property not found)")
            }
        } catch (e: Exception) {
            Log.e("TuningRepo_Vulkan", "Exception getting Vulkan API Version: ${e.message}", e)
            emit("N/A (Error)")
        }
    }.flowOn(Dispatchers.IO)

    /* ---------- Private Shell Helpers ---------- */
    private fun runShellCommand(cmd: String) {
        Log.d("TuningRepository", "[Shell RW] Executing: '$cmd'")
        val result = Shell.cmd(cmd).exec()
        if (result.isSuccess) {
            Log.i("TuningRepository", "[Shell RW] Success (code ${result.code}): '$cmd'")
        } else {
            Log.e("TuningRepository", "[Shell RW] Failed (code ${result.code}): '$cmd'. Err: ${result.err.joinToString("\n")}. Out: ${result.out.joinToString("\n")}")
        }
    }

    private fun readShellCommand(cmd: String): String {
        Log.d("TuningRepository", "[Shell R] Executing: '$cmd'")
        val result = Shell.cmd(cmd).exec()
        return if (result.isSuccess) {
            val output = result.out.joinToString("\n")
            if (output.isBlank() && cmd.startsWith("cat")) {
                Log.w("TuningRepository", "[Shell R] Command '$cmd' success but output is blank.")
            }
            output
        } else {
            Log.e("TuningRepository", "[Shell R] Failed (code ${result.code}): '$cmd'. Err: ${result.err.joinToString("\n")}. Out: ${result.out.joinToString("\n")}")
            ""
        }
    }

    /* ---------- Device Control ---------- */
    fun rebootDevice(reason: String? = null): Flow<Boolean> = flow {
        val command = if (reason.isNullOrBlank()) "reboot" else "reboot $reason"
        Log.d("TuningRepository", "Attempting to reboot device. Reason: ${reason ?: "N/A"}")
        try {
            val result = Shell.cmd(command).exec()
            if (result.isSuccess) {
                emit(true)
            } else {
                Log.e("TuningRepository", "Reboot command failed. Code: ${result.code}, Err: ${result.err.joinToString("\n")}")
                emit(false)
            }
        } catch (e: Exception) {
            Log.e("TuningRepository", "Exception during reboot: ${e.message}", e)
            emit(false)
        }
    }.flowOn(Dispatchers.IO)
}
