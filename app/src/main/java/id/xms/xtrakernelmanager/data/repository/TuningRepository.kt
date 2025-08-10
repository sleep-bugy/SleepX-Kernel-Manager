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
import java.io.DataOutputStream
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
    private var isSuShellWorking = true // Assume libsu works initially

    // Fungsi untuk menjalankan perintah shell dengan penanganan SELinux
    // Fungsi ini akan menjadi wrapper untuk semua perintah yang MEMBUTUHKAN perubahan SELinux
    private fun runTuningCommand(cmd: String): Boolean {
        Log.d(TAG, "[Shell RW TUNING] Preparing to execute: '$cmd'")
        val originalSelinuxMode = getSelinuxModeInternal() // Dapatkan mode SELinux saat ini
        var success = false

        // Hanya ubah ke permisif jika saat ini enforcing
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)

        if (needsSelinuxChange) {
            Log.i(TAG, "[Shell RW TUNING] Current SELinux mode: $originalSelinuxMode. Setting to Permissive.")
            if (!setSelinuxModeInternal(false)) { // Set ke Permissive (0)
                Log.e(TAG, "[Shell RW TUNING] Failed to set SELinux to Permissive. Command will run in current mode.")
                // Lanjutkan eksekusi perintah meskipun gagal mengubah SELinux,
                // mungkin beberapa perintah masih bisa berhasil atau ini adalah fallback.
            }
        }

        // Jalankan perintah tuning utama
        success = executeShellCommand(cmd)

        // Kembalikan ke mode SELinux enforcing jika sebelumnya diubah
        if (needsSelinuxChange) {
            Log.i(TAG, "[Shell RW TUNING] Restoring SELinux mode to Enforcing.")
            if (!setSelinuxModeInternal(true)) { // Set kembali ke Enforcing (1)
                Log.w(TAG, "[Shell RW TUNING] Failed to restore SELinux to Enforcing. System might remain in Permissive mode.")
                // Tidak ada tindakan error khusus di sini, hanya log peringatan.
            }
        }
        return success
    }

    // Fungsi internal untuk eksekusi shell, dipanggil oleh runTuningCommand atau langsung jika tidak perlu SELinux handling
    private fun executeShellCommand(cmd: String): Boolean {
        Log.d(TAG, "[Shell EXEC] Executing: '$cmd'")
        if (isSuShellWorking) {
            try {
                val result = Shell.cmd(cmd).exec()
                return if (result.isSuccess) {
                    Log.i(TAG, "[Shell EXEC] Success (libsu, code ${result.code}): '$cmd'")
                    true
                } else {
                    Log.e(TAG, "[Shell EXEC] Failed (libsu, code ${result.code}): '$cmd'. Err: ${result.err.joinToString("\n")}")
                    isSuShellWorking = false
                    executeShellCommandFallback(cmd)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Shell EXEC] Exception with libsu for cmd: '$cmd'. Trying fallback.", e)
                isSuShellWorking = false
                return executeShellCommandFallback(cmd)
            }
        } else {
            return executeShellCommandFallback(cmd)
        }
    }


    // Fungsi runShellCommand lama sekarang bisa menjadi alias ke executeShellCommand
    // jika Anda ingin membedakan antara perintah yang dimodifikasi SELinux dan yang tidak.
    // Atau, Anda bisa mengganti semua pemanggilan runShellCommand lama dengan runTuningCommand.
    // Untuk kesederhanaan, saya akan asumsikan semua perintah tulis potensial akan melalui runTuningCommand.
    // Jika ada perintah tulis yang TIDAK boleh mengubah SELinux, panggil executeShellCommand secara langsung.
    private fun runShellCommand(cmd: String): Boolean {
        // Wrapper ini bisa dipertimbangkan apakah akan selalu melalui runTuningCommand
        // atau executeShellCommand. Jika sebuah `echo` sederhana tidak perlu SELinux,
        // maka bisa langsung ke `executeShellCommand`.
        // Untuk saat ini, kita akan membuat semua `runShellCommand` yang ada
        // menggunakan logika tuning SELinux.
        return runTuningCommand(cmd)
    }


    private fun readShellCommand(cmd: String): String {
        Log.d(TAG, "[Shell R] Executing: '$cmd'")
        if (isSuShellWorking) {
            try {
                val result = Shell.cmd(cmd).exec()
                return if (result.isSuccess) result.out.joinToString("\n").trim() else {
                    isSuShellWorking = false // Assume problem if read fails too
                    readShellCommandFallback(cmd)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Shell R] Exception with libsu for cmd: '$cmd'. Trying fallback.", e)
                isSuShellWorking = false
                return readShellCommandFallback(cmd)
            }
        } else {
            return readShellCommandFallback(cmd)
        }
    }

    /* ----------------------------------------------------------
       SELinux Specific Helpers (Internal)
       ---------------------------------------------------------- */
    private fun setSelinuxModeInternal(enforcing: Boolean): Boolean {
        val mode = if (enforcing) "1" else "0"
        Log.i(TAG, "[SELinux] Setting mode to: ${if (enforcing) "Enforcing" else "Permissive"} ($mode)")
        // Gunakan executeShellCommand di sini karena ini adalah bagian dari mekanisme internal
        // dan tidak boleh memicu loop rekursif perubahan SELinux.
        val success = executeShellCommand("setenforce $mode")
        if (!success) {
            Log.e(TAG, "[SELinux] Failed to set SELinux mode to $mode")
        }
        return success
    }

    private fun getSelinuxModeInternal(): String {
        // Gunakan readShellCommand karena ini adalah operasi baca
        val result = readShellCommand("getenforce").trim()
        Log.i(TAG, "[SELinux] Current mode: $result")
        return result // Akan mengembalikan "Enforcing", "Permissive", atau "Disabled"
    }

    // Public functions for SELinux if needed by ViewModel/UI, though likely not directly
    fun setSelinuxEnforcing(): Flow<Boolean> = flow {
        emit(setSelinuxModeInternal(true))
    }.flowOn(Dispatchers.IO)

    fun setSelinuxPermissive(): Flow<Boolean> = flow {
        emit(setSelinuxModeInternal(false))
    }.flowOn(Dispatchers.IO)

    fun getCurrentSelinuxMode(): Flow<String> = flow {
        emit(getSelinuxModeInternal())
    }.flowOn(Dispatchers.IO)


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

    fun calculateMaxZramSize(): Long {
        val ramGB = ceil(totalRamBytes / (1024.0 * 1024.0 * 1024.0)).toLong()
        return when {
            ramGB <= 3L -> 1_073_741_824L   // 1 GB
            ramGB <= 4L -> 2_147_483_648L   // 2 GB
            ramGB <= 6L -> 4_294_967_296L   // 4 GB
            ramGB <= 8L -> 9_663_676_416L   // 9 GB
            ramGB <= 12L -> 15_032_385_536L // 14 GB
            ramGB <= 16L -> 19_327_352_832L // 18 GB
            else -> totalRamBytes / 4       // 25%
        }
    }

    // Fungsi resizeZramSafely sekarang akan menggunakan runTuningCommand untuk setiap echo
    // Namun, karena ini adalah serangkaian perintah, lebih baik membungkus seluruh blok.
    private fun resizeZramSafely(newSizeBytes: Long): Boolean {
        // Perintah-perintah ini melakukan penulisan, jadi bungkus dengan SELinux handling
        val originalSelinuxMode = getSelinuxModeInternal()
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)
        var overallSuccess = true

        if (needsSelinuxChange) {
            Log.i(TAG, "[ZRAM SELinux] Current SELinux mode: $originalSelinuxMode. Setting to Permissive for ZRAM ops.")
            if (!setSelinuxModeInternal(false)) {
                Log.e(TAG, "[ZRAM SELinux] Failed to set SELinux to Permissive for ZRAM ops.")
                // Lanjutkan, tapi catat kegagalan
            }
        }

        // Jalankan perintah ZRAM satu per satu menggunakan executeShellCommand (tanpa handling SELinux individual)
        // karena kita sudah menangani SELinux untuk seluruh blok.
        if (!executeShellCommand("swapoff /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
        if (!executeShellCommand("echo 1 > $zramResetPath")) overallSuccess = false
        if (!executeShellCommand("echo $newSizeBytes > $zramDisksizePath")) overallSuccess = false
        if (!executeShellCommand("mkswap /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
        if (!executeShellCommand("swapon /dev/block/zram0 2>/dev/null || true")) overallSuccess = false

        if (needsSelinuxChange) {
            Log.i(TAG, "[ZRAM SELinux] Restoring SELinux mode to Enforcing after ZRAM ops.")
            if (!setSelinuxModeInternal(true)) {
                Log.w(TAG, "[ZRAM SELinux] Failed to restore SELinux to Enforcing after ZRAM ops.")
            }
        }
        return overallSuccess
    }

    fun setZramEnabled(enabled: Boolean): Flow<Boolean> = flow {
        // resizeZramSafely sudah menangani SELinux
        resizeZramSafely(if (enabled) calculateMaxZramSize() else 0)
        // Baca status setelahnya, tidak perlu SELinux handling untuk `cat`
        emit(readShellCommand("cat $zramInitStatePath").trim() == "1")
    }.flowOn(Dispatchers.IO)

    fun setZramDisksize(sizeBytes: Long): Boolean {
        // resizeZramSafely sudah menangani SELinux
        return resizeZramSafely(sizeBytes)
    }

    fun getZramDisksize(): Flow<Long> = flow {
        emit(readShellCommand("cat $zramDisksizePath").toLongOrNull() ?: 0L)
    }.flowOn(Dispatchers.IO)

    fun getZramEnabled(): Flow<Boolean> = flow {
        emit(readShellCommand("cat $zramInitStatePath").trim() == "1")
    }.flowOn(Dispatchers.IO)

    fun setCompressionAlgorithm(algo: String): Boolean {
        val currentSize = readShellCommand("cat $zramDisksizePath").toLongOrNull() ?: 0L
        if (readShellCommand("if [ -e $zramControlPath ]; then echo 1; else echo 0; fi").trim() != "1") {
            Log.w(TAG, "ZRAM node $zramControlPath does not exist. Cannot set compression algorithm.")
            return false
        }

        // Tangani SELinux untuk blok perintah ini
        val originalSelinuxMode = getSelinuxModeInternal()
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)
        var success = true

        if (needsSelinuxChange) {
            setSelinuxModeInternal(false) // Ke Permisif
        }

        // Jalankan perintah dengan executeShellCommand
        executeShellCommand("chmod 666 $zramCompAlgorithmPath") // Chmod mungkin memerlukan permisif

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
            "echo $algo > $zramCompAlgorithmPath"
        }

        // Eksekusi perintah utama, bisa jadi multi-line
        // Jika `commands` adalah string tunggal dengan newline, Shell.cmd() akan menanganinya.
        // Jika Anda ingin mengeksekusi baris per baris, Anda perlu mem-parsingnya.
        // Untuk saat ini, kita asumsikan Shell.cmd() menangani string multi-baris.
        success = executeShellCommand(commands)

        if (needsSelinuxChange) {
            setSelinuxModeInternal(true) // Kembali ke Enforcing
        }
        return success
    }

    fun getCompressionAlgorithms(): Flow<List<String>> = flow {
        emit(readShellCommand("cat $zramCompAlgorithmPath").split(" ").filter { it.isNotBlank() }.sorted())
    }.flowOn(Dispatchers.IO)

    fun getCurrentCompression(): Flow<String> = flow {
        val raw = readShellCommand("cat $zramCompAlgorithmPath").trim()
        val active = raw.split(" ").firstOrNull { it.startsWith("[") }
            ?.removeSurrounding("[", "]")
            ?: raw.split(" ").firstOrNull()
            ?: "lz4" // Default jika parsing gagal
        emit(active)
    }.flowOn(Dispatchers.IO)

    // Untuk fungsi set sederhana yang hanya melakukan 'echo', runTuningCommand akan menangani SELinux
    fun setSwappiness(value: Int): Boolean =
        runTuningCommand("echo $value > $swappinessPath")

    fun getSwappiness(): Flow<Int> = flow {
        emit(readShellCommand("cat $swappinessPath").toIntOrNull() ?: 60)
    }.flowOn(Dispatchers.IO)

    fun setDirtyRatio(value: Int): Boolean =
        runTuningCommand("echo $value > $dirtyRatioPath")

    fun getDirtyRatio(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyRatioPath").toIntOrNull() ?: 20)
    }.flowOn(Dispatchers.IO)

    fun setDirtyBackgroundRatio(value: Int): Boolean =
        runTuningCommand("echo $value > $dirtyBackgroundRatioPath")

    fun getDirtyBackgroundRatio(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyBackgroundRatioPath").toIntOrNull() ?: 10)
    }.flowOn(Dispatchers.IO)

    // valueCentisecs sudah benar di sini, ViewModel yang akan mengonversi dari detik jika perlu
    fun setDirtyWriteback(valueCentisecs: Int): Boolean =
        runTuningCommand("echo $valueCentisecs > $dirtyWritebackCentisecsPath")

    fun getDirtyWriteback(): Flow<Int> = flow {
        // Kembalikan dalam DETIK untuk ViewModel
        emit((readShellCommand("cat $dirtyWritebackCentisecsPath").toIntOrNull() ?: 3000) / 100)
    }.flowOn(Dispatchers.IO)

    // valueCentisecsInput sudah benar, ViewModel yang akan mengonversi dari detik
    fun setDirtyExpireCentisecs(valueCentisecsInput: Int): Boolean =
        runTuningCommand("echo $valueCentisecsInput > $dirtyExpireCentisecsPath")

    fun getDirtyExpireCentisecs(): Flow<Int> = flow {
        // Kembalikan dalam DETIK untuk ViewModel
        emit((readShellCommand("cat $dirtyExpireCentisecsPath").toIntOrNull() ?: 3000) / 100)
    }.flowOn(Dispatchers.IO)

    // valueKBytes sudah benar, ViewModel yang akan mengonversi dari MB
    fun setMinFreeMemory(valueKBytes: Int): Boolean =
        runTuningCommand("echo $valueKBytes > $minFreeKbytesPath")

    fun getMinFreeMemory(): Flow<Int> = flow {
        // Kembalikan dalam MB untuk ViewModel
        emit((readShellCommand("cat $minFreeKbytesPath").toIntOrNull() ?: (128 * 1024)) / 1024)
    }.flowOn(Dispatchers.IO)


    /* ----------------------------------------------------------
       CPU
       ---------------------------------------------------------- */
    fun getCpuGov(cluster: String): Flow<String> = flow {
        emit(readShellCommand("cat ${cpuGovPath.format(cluster)}"))
    }.flowOn(Dispatchers.IO)

    fun setCpuGov(cluster: String, gov: String): Boolean {
        // `chmod` dan `echo` keduanya memerlukan SELinux permisif jika enforcing
        // runTuningCommand akan menangani ini untuk setiap perintah.
        // Ini mungkin kurang efisien karena SELinux akan diubah dua kali.
        // Idealnya, buat fungsi yang menjalankan beberapa perintah dalam satu blok SELinux.
        // Untuk saat ini, kita gunakan pendekatan sederhana.
        val chmodSuccess = runTuningCommand("chmod 666 ${cpuGovPath.format(cluster)}")
        return chmodSuccess && runTuningCommand("echo $gov > ${cpuGovPath.format(cluster)}")
    }

    fun getCpuFreq(cluster: String): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat ${cpuMinFreqPath.format(cluster)}").toIntOrNull() ?: 0
        val max = readShellCommand("cat ${cpuMaxFreqPath.format(cluster)}").toIntOrNull() ?: 0
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun setCpuFreq(cluster: String, min: Int, max: Int): Boolean {
        // Sama seperti setCpuGov, idealnya satu blok SELinux
        val chmodMinSuccess = runTuningCommand("chmod 666 ${cpuMinFreqPath.format(cluster)}")
        val chmodMaxSuccess = runTuningCommand("chmod 666 ${cpuMaxFreqPath.format(cluster)}")
        val setMinSuccess = runTuningCommand("echo $min > ${cpuMinFreqPath.format(cluster)}")
        val setMaxSuccess = runTuningCommand("echo $max > ${cpuMaxFreqPath.format(cluster)}")
        return chmodMinSuccess && chmodMaxSuccess && setMinSuccess && setMaxSuccess
    }

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

    fun setCoreOnline(coreId: Int, isOnline: Boolean): Boolean {
        val value = if (isOnline) 1 else 0
        // Chmod mungkin diperlukan dan harus berada dalam blok SELinux jika digunakan
        var chmodSuccess = true
        if (coreId >= 4) { // Asumsi dari kode sebelumnya
            // chmod juga akan melalui runTuningCommand jika itu yang terbaik
            chmodSuccess = runTuningCommand("chmod 666 ${coreOnlinePath.format(coreId)}")
        }
        return chmodSuccess && runTuningCommand("echo $value > ${coreOnlinePath.format(coreId)}")
    }

    fun getCoreOnline(coreId: Int): Boolean =
        readShellCommand("cat ${coreOnlinePath.format(coreId)}").trim() == "1"

    // Helper untuk mendapatkan jumlah core, jika tidak ada cara yang lebih baik
    fun getNumberOfCores(): Int {
        // Coba baca dari /sys/devices/system/cpu/present atau online
        // Ini adalah contoh sederhana, mungkin perlu disesuaikan
        val presentCores = readShellCommand("cat /sys/devices/system/cpu/present") // e.g., "0-7"
        if (presentCores.contains("-")) {
            try {
                return presentCores.split("-").last().toInt() + 1
            } catch (e: NumberFormatException) {
                // Fallback jika format tidak terduga
            }
        }
        // Fallback ke hitungan file cpuX, bisa lambat atau tidak akurat
        var count = 0
        while (readShellCommand("if [ -d /sys/devices/system/cpu/cpu$count ]; then echo 1; else echo 0; fi").trim() == "1") {
            count++
        }
        return if (count > 0) count else 8 // Default ke 8 jika gagal deteksi
    }


    /* ----------------------------------------------------------
       GPU
       ---------------------------------------------------------- */
    fun getGpuGov(): Flow<String> = flow {
        emit(readShellCommand("cat $gpuGovPath"))
    }.flowOn(Dispatchers.IO)

    fun setGpuGov(gov: String): Boolean {
        val chmodSuccess = runTuningCommand("chmod 666 $gpuGovPath")
        return chmodSuccess && runTuningCommand("echo $gov > $gpuGovPath")
    }

    fun getGpuFreq(): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat $gpuMinFreqPath").toIntOrNull() ?: 0
        val max = readShellCommand("cat $gpuMaxFreqPath").toIntOrNull() ?: 0
        emit((min / 1000000) to (max / 1000000)) // Pastikan pembagian benar (Hz ke MHz)
    }.flowOn(Dispatchers.IO)

    fun setGpuMinFreq(freqMHz: Int): Boolean =
        runTuningCommand("echo ${freqMHz * 1000000} > $gpuMinFreqPath") // MHz ke Hz

    fun setGpuMaxFreq(freqMHz: Int): Boolean =
        runTuningCommand("echo ${freqMHz * 1000000} > $gpuMaxFreqPath") // MHz ke Hz


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
            .map { it / 1000000 } // Hz ke MHz
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getGpuPowerLevelRange(): Flow<Pair<Float, Float>> = flow {
        // Asumsi nilai power level adalah integer
        val min = readShellCommand("cat $gpuMinPowerLevelPath").toFloatOrNull() ?: 0f
        val max = readShellCommand("cat $gpuMaxPowerLevelPath").toFloatOrNull() ?: 0f
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun getCurrentGpuPowerLevel(): Flow<Float> = flow {
        emit(readShellCommand("cat $gpuCurrentPowerLevelPath").toFloatOrNull() ?: 0f)
    }.flowOn(Dispatchers.IO)

    fun setGpuPowerLevel(level: Float): Boolean =
        runTuningCommand("echo ${level.toInt()} > $gpuCurrentPowerLevelPath")


    /* ----------------------------------------------------------
       Thermal
       ---------------------------------------------------------- */
    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        // Jika pembacaan gagal, -1 adalah indikator yang baik
        emit(readShellCommand("cat $thermalSysfsNode").toIntOrNull() ?: -1)
    }.flowOn(Dispatchers.IO)

    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        // Menggunakan runTuningCommand untuk setiap operasi tulis
        // Idealnya, bungkus dalam satu blok jika memungkinkan, tapi untuk setprop ini mungkin tidak apa-apa
        val preChmodOk = runTuningCommand("chmod 0666 $thermalSysfsNode")
        val writeOk = runTuningCommand("echo $modeIndex > $thermalSysfsNode")
        // chmod lagi setelahnya mungkin tidak selalu diperlukan, tergantung implementasi kernel
        // tapi tidak berbahaya.
        val postChmodOk = runTuningCommand("chmod 0666 $thermalSysfsNode")
        emit(preChmodOk && writeOk && postChmodOk)
    }.flowOn(Dispatchers.IO)

    /* ----------------------------------------------------------
       OpenGL / Vulkan / Renderer
       ---------------------------------------------------------- */
    fun getOpenGlesDriver(): Flow<String> = flow {
        val rawOutput = readShellCommand("dumpsys SurfaceFlinger | grep \"GLES:\"")
        val glesInfo = rawOutput.substringAfter("GLES:", "").trim()
        emit(
            if (glesInfo.isNotBlank()) glesInfo else "N/A"
        )
    }.flowOn(Dispatchers.IO)

    fun getGpuRenderer(): Flow<String> = flow {
        emit(readShellCommand("getprop debug.hwui.renderer").ifEmpty { "OpenGL" }) // Default OpenGL
    }.flowOn(Dispatchers.IO)

    fun setGpuRenderer(renderer: String): Flow<Boolean> = flow {
        val command = if (renderer.equals("Default", ignoreCase = true)) {
            "setprop debug.hwui.renderer \"\"" // Set ke string kosong untuk default
        } else {
            "setprop debug.hwui.renderer $renderer"
        }
        // `setprop` biasanya tidak memerlukan SELinux permisif, jadi bisa gunakan `executeShellCommand`
        // Tapi untuk konsistensi, jika `runTuningCommand` defaultnya, bisa dipakai.
        // Mari kita asumsikan setprop aman tanpa SELinux change.
        emit(executeShellCommand(command))
    }.flowOn(Dispatchers.IO)

    fun getVulkanApiVersion(): Flow<String> = flow {
        emit(readShellCommand("getprop ro.hardware.vulkan.version").ifEmpty { "N/A" })
    }.flowOn(Dispatchers.IO)

    fun rebootDevice(): Flow<Boolean> = flow {
        // Reboot tidak memerlukan SELinux permisif secara khusus.
        emit(executeShellCommand("reboot"))
    }.flowOn(Dispatchers.IO)


    /* ----------------------------------------------------------
       Fallback Shell using Runtime.exec
       ---------------------------------------------------------- */
    private fun executeShellCommandFallback(cmd: String): Boolean {
        Log.d(TAG, "[Shell EXEC Fallback] Executing: '$cmd'")
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            // Penting untuk mengonsumsi output dan error stream untuk menghindari deadlock
            // Meskipun kita tidak menggunakannya untuk Boolean, ini adalah praktik yang baik.
            process.inputStream.bufferedReader().use { it.readText() } // Konsumsi output
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() } // Konsumsi error

            val exitCode = process.waitFor()
            return if (exitCode == 0) {
                Log.i(TAG, "[Shell EXEC Fallback] Success (code $exitCode): '$cmd'")
                true
            } else {
                Log.e(TAG, "[Shell EXEC Fallback] Failed (code $exitCode): '$cmd'. Err: $errorOutput")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Shell EXEC Fallback] Exception for cmd: '$cmd'", e)
            return false
        }
    }

    private fun readShellCommandFallback(cmd: String): String {
        Log.d(TAG, "[Shell R Fallback] Executing: '$cmd'")
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            return if (exitCode == 0) {
                output.trim()
            } else {
                Log.e(TAG, "[Shell R Fallback] Failed (code $exitCode) for cmd: '$cmd'. Err: $errorOutput")
                "" // Kembalikan string kosong jika gagal
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Shell R Fallback] Exception for cmd: '$cmd'", e)
            return "" // Kembalikan string kosong jika terjadi exception
        }
    }
}
