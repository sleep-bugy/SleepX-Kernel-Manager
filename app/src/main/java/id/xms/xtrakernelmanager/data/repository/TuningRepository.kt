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
    private val defaultThermalNodePermissions = "0664"
    private val temporaryWritablePermissions = "0666"

    /* ---------- Thermal Control (Strategi FKM) ---------- */

    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        val commands = listOf(
            "chmod $temporaryWritablePermissions $thermalSysfsNode",
            "echo $modeIndex > $thermalSysfsNode",
            "chmod $defaultThermalNodePermissions $thermalSysfsNode"
        )

        Log.d("TuningRepository", "[R/W] Attempting to set thermal mode index to $modeIndex using FKM strategy.")
        Log.d("TuningRepository", "[R/W] Commands: \n${commands.joinToString("\n")}")

        try {
            val chmodWritableResult = Shell.cmd(commands[0]).exec()
            if (!chmodWritableResult.isSuccess) {
                Log.e(
                    "TuningRepository",
                    "[R/W] Failed at chmod writable step. Cmd: '${commands[0]}'. Code: ${chmodWritableResult.code}. Err: ${chmodWritableResult.err.joinToString("\n")}. Out: ${chmodWritableResult.out.joinToString("\n")}"
                )
                emit(false)
                return@flow
            }
            Log.i("TuningRepository", "[R/W] Chmod writable success: ${commands[0]}")

            val echoResult = Shell.cmd(commands[1]).exec()
            var valueSuccessfullySet = false
            if (echoResult.isSuccess) {
                // Verifikasi nilai
                val verificationResult = Shell.cmd("cat $thermalSysfsNode").exec()
                val currentValue = verificationResult.out.firstOrNull()?.trim()
                if (verificationResult.isSuccess && currentValue == modeIndex.toString()) {
                    Log.i("TuningRepository", "[R/W] Successfully wrote and verified value $modeIndex. Cmd: '${commands[1]}'")
                    valueSuccessfullySet = true
                } else {
                    Log.w(
                        "TuningRepository",
                        "[R/W sconfig] Echo command '${commands[1]}' reported success (code ${echoResult.code}), but verification failed. Current value: '$currentValue', Expected: '$modeIndex'. Echo Err: ${echoResult.err.joinToString("\n")}. Echo Out: ${echoResult.out.joinToString("\n")}. Cat Err: ${verificationResult.err.joinToString("\n")}"
                    )
                }
            } else {
                Log.e(
                    "TuningRepository",
                    "[R/W sconfig] Failed at echo step. Cmd: '${commands[1]}'. Code: ${echoResult.code}. Err: ${echoResult.err.joinToString("\n")}. Out: ${echoResult.out.joinToString("\n")}"
                )
            }

            // Selalu coba kembalikan izin, bahkan jika echo gagal, untuk keamanan.
            val chmodRestoreResult = Shell.cmd(commands[2]).exec()
            if (!chmodRestoreResult.isSuccess) {
                Log.w(
                    "TuningRepository",
                    "[Thermal FKM Style] Failed to restore permissions. Cmd: '${commands[2]}'. Code: ${chmodRestoreResult.code}. Err: ${chmodRestoreResult.err.joinToString("\n")}. Out: ${chmodRestoreResult.out.joinToString("\n")}"
                )

            } else {
                Log.i("TuningRepository", "[R/W] Restored permissions success: ${commands[2]}")
            }

            emit(valueSuccessfullySet)

        } catch (e: Exception) {
            Log.e("TuningRepository", "[R/W] ExceptionMthermal set: ${e.message}", e)
            emit(false)
        }
    }.flowOn(Dispatchers.IO)


    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        val command = "cat $thermalSysfsNode"
        Log.d("TuningRepository", "[Thermal Read] Attempting to read thermal mode index with libsu: '$command'")
        try {
            val result = Shell.cmd(command).exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                val currentValueString = result.out.first().trim()
                val currentValue = currentValueString.toIntOrNull()
                if (currentValue != null) {
                    emit(currentValue)
                    Log.d("TuningRepository", "[Thermal Read] Current thermal mode index from $thermalSysfsNode: $currentValue")
                } else {
                    Log.e("TuningRepository", "[Thermal Read] Failed to parse thermal mode index from: '$currentValueString' at $thermalSysfsNode. Raw output: ${result.out.joinToString("\\n")}")
                    emit(-1)
                }
            } else {
                Log.e(
                    "TuningRepository",
                    "[Thermal Read] Failed to read current thermal mode index from $thermalSysfsNode. Command: '$command'. Code: ${result.code}. Error: ${result.err.joinToString("\n")}. Stdout: ${result.out.joinToString("\n")}"
                )
                emit(-1)
            }
        } catch (e: Exception) {
            Log.e("TuningRepository", "[Thermal Read] Exception reading current thermal mode index from $thermalSysfsNode with command '$command': ${e.message}", e)
            emit(-1)
        }
    }.flowOn(Dispatchers.IO)

    fun setCpuGov(cluster: String, gov: String) =
        runShellCommand("echo $gov > /sys/devices/system/cpu/$cluster/cpufreq/scaling_governor")

    fun setCpuFreq(cluster: String, min: Int, max: Int) {
        runShellCommand("echo $min > /sys/devices/system/cpu/$cluster/cpufreq/scaling_min_freq")
        runShellCommand("echo $max > /sys/devices/system/cpu/$cluster/cpufreq/scaling_max_freq")
    }

    fun getCpuGov(cluster: String): Flow<String> = flow {
        val result = readShellCommand("cat /sys/devices/system/cpu/$cluster/cpufreq/scaling_governor")
        emit(result.trim())
    }.flowOn(Dispatchers.IO)

    fun getAvailableCpuGovernors(cluster: String): Flow<List<String>> = flow {
        val path = "/sys/devices/system/cpu/$cluster/cpufreq/scaling_available_governors"
        try {
            val command = "cat $path"
            val governorsString = readShellCommand(command).trim()
            if (governorsString.isNotEmpty()) {
                val governorsList = governorsString.split(" ").filter { it.isNotBlank() }.sorted()
                emit(governorsList)
            } else {
                Log.w("TuningRepository", "No available CPU governors found or error reading for $cluster from $path.")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("TuningRepository", "Exception reading available CPU governors for $cluster: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getCpuFreq(cluster: String): Flow<Pair<Int, Int>> = flow {
        val minPath = "/sys/devices/system/cpu/$cluster/cpufreq/scaling_min_freq"
        val maxPath = "/sys/devices/system/cpu/$cluster/cpufreq/scaling_max_freq"
        val min = readShellCommand("cat $minPath").trim().toIntOrNull() ?: 0
        val max = readShellCommand("cat $maxPath").trim().toIntOrNull() ?: 0
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun getAvailableCpuFrequencies(cluster: String): Flow<List<Int>> = flow {
        val path = "/sys/devices/system/cpu/$cluster/cpufreq/scaling_available_frequencies"
        try {
            val command = "cat $path"
            val frequenciesString = readShellCommand(command).trim()
            if (frequenciesString.isNotEmpty()) {
                val frequenciesList = frequenciesString.split(" ")
                    .mapNotNull { it.toIntOrNull() }
                    .sorted()
                emit(frequenciesList)
            } else {
                Log.w("TuningRepository", "No available CPU frequencies found or error reading for $cluster from $path.")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("TuningRepository", "Exception reading available CPU frequencies for $cluster: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)


    fun setGpuGov(gov: String) =
        runShellCommand("echo $gov > /sys/class/kgsl/kgsl-3d0/devfreq/governor")

    fun setGpuFreq(min: Int, max: Int) {
        runShellCommand("echo $min > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq")
        runShellCommand("echo $max > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq")
    }

    fun getGpuGov(): Flow<String> = flow {
        val result = readShellCommand("cat /sys/class/kgsl/kgsl-3d0/devfreq/governor")
        emit(result.trim())
    }.flowOn(Dispatchers.IO)

    fun getAvailableGpuGovernors(): Flow<List<String>> = flow {
        val path = "/sys/class/kgsl/kgsl-3d0/devfreq/available_governors"
        try {
            val command = "cat $path"
            val governorsString = readShellCommand(command).trim()
            if (governorsString.isNotEmpty()){
                val governorsList = governorsString.split(" ").filter { it.isNotBlank() }.sorted()
                emit(governorsList)
            } else {
                Log.w("TuningRepository", "No available GPU governors found or error reading from $path.")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("TuningRepository", "Exception reading available GPU governors: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getGpuFreq(): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat /sys/class/kgsl/kgsl-3d0/devfreq/min_freq").trim().toIntOrNull() ?: 0
        val max = readShellCommand("cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq").trim().toIntOrNull() ?: 0
        emit(min to max)
    }.flowOn(Dispatchers.IO)


    fun setSwappiness(value: Int) =
        runShellCommand("echo $value > /proc/sys/vm/swappiness")

    fun getSwappiness(): Flow<Int> = flow {
        try {
            val command = "cat /proc/sys/vm/swappiness"
            val resultString = readShellCommand(command).trim()
            val result = resultString.toIntOrNull()
            if (result != null) {
                emit(result)
            } else {
                Log.e("TuningRepository", "Failed to parse swappiness value from '$resultString'. Command: '$command'")
                emit(60) // Emit default 60 jika gagal parse
            }
        } catch (e: Exception) {
            Log.e("TuningRepository", "Exception reading swappiness: ${e.message}", e)
            emit(60) // Default value on error
        }
    }.flowOn(Dispatchers.IO)


    /* ---------- Private Shell Helpers (Berbasis libsu, digunakan untuk NON-THERMAL) ---------- */

    private fun runShellCommand(cmd: String) {
        // ... (runShellCommand tidak berubah)
        Log.d("TuningRepository", "[Helper LibSU] Attempting write command: '$cmd'")
        val result = Shell.cmd(cmd).exec()
        if (result.isSuccess) {
            Log.i("TuningRepository", "[Helper LibSU] Command success (exit code ${result.code}): '$cmd'")
        } else {
            Log.e(
                "TuningRepository",
                "[Helper LibSU] Command failed: '$cmd'. Code: ${result.code}. Error: ${result.err.joinToString("\n")}. Stdout: ${result.out.joinToString("\n")}"
            )
        }
    }

    private fun readShellCommand(cmd: String): String {
        // ... (readShellCommand tidak berubah)
        Log.d("TuningRepository", "[Helper LibSU] Attempting read command: '$cmd'")
        val result = Shell.cmd(cmd).exec()
        return if (result.isSuccess) {
            result.out.joinToString("\n").also {
                if (it.isBlank() && cmd.startsWith("cat")) Log.w("TuningRepository", "[Helper LibSU] Command '$cmd' success but output is blank.")
            }
        } else {
            Log.e(
                "TuningRepository",
                "[Helper LibSU] Command failed: '$cmd'. Code: ${result.code}. Error: ${result.err.joinToString("\n")}. Stdout: ${result.out.joinToString("\n")}"
            )
            ""
        }
    }
}
