package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.vector.path
// Hapus impor yang tidak terpakai jika ada, seperti:
// import androidx.compose.ui.geometry.isEmpty
// import androidx.compose.ui.graphics.vector.path
// import kotlin.io.path.inputStream // Ini juga sepertinya tidak digunakan, File.inputStream() lebih umum

import id.xms.xtrakernelmanager.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose // Diperlukan untuk callbackFlow
import kotlinx.coroutines.channels.ChannelResult // Untuk memeriksa hasil trySend
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
// import kotlinx.coroutines.flow.first // Tidak digunakan di kode yang Anda berikan
// import kotlinx.coroutines.flow.mapLatest // Tidak digunakan di kode yang Anda berikan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.inputStream


@Suppress("UNREACHABLE_CODE")
@Singleton
class SystemRepository @Inject constructor(
    private val context: Context,
) {

    companion object {
        private const val TAG = "SystemRepository"
        private const val VALUE_NOT_AVAILABLE = "N/A"
        private const val VALUE_UNKNOWN = "Unknown"
        private const val REALTIME_UPDATE_INTERVAL_MS = 1000L
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var cachedSystemInfo: SystemInfo? = null
    private suspend fun getCachedSystemInfo(): SystemInfo {
        // Menggunakan double-checked locking untuk thread-safety sederhana jika diakses dari coroutine berbeda
        // Meskipun dalam kasus ini, kemungkinan besar akan dipanggil dari scope callbackFlow yang sama.
        return cachedSystemInfo ?: synchronized(this) {
            cachedSystemInfo ?: getSystemInfoInternal().also { cachedSystemInfo = it }
        }
    }

    private fun readFileToString(filePath: String, fileDescription: String, attemptSu: Boolean = true): String? {
        val file = File(filePath)
        try {
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                if (content.isNotBlank()) {
                    return content
                } else {
                    Log.w(TAG, "'$fileDescription': File kosong (langsung). Path: $filePath")
                    if (!attemptSu) return null
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "'$fileDescription': SecurityException (langsung). Path: $filePath. Mencoba SU.", e)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "'$fileDescription': FileNotFoundException (langsung). Path: $filePath. Mencoba SU.", e)
        } catch (e: IOException) {
            Log.e(TAG, "'$fileDescription': IOException (langsung). Path: $filePath.", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "'$fileDescription': Exception tidak diketahui (langsung). Path: $filePath.", e)
            return null
        }

        if (attemptSu) {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat \"$filePath\""))
                // Membaca output dan error stream dalam coroutine terpisah untuk menghindari deadlock
                // Namun, untuk kesederhanaan di sini, kita jaga seperti sebelumnya,
                // asumsikan output tidak terlalu besar.
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                val exitCode = process.waitFor()
                val errorOutput = StringBuilder()
                while (errorReader.readLine().also { line = it } != null) {
                    errorOutput.append(line).append("\n")
                }

                if (errorOutput.isNotBlank()) {
                    Log.w(TAG, "'$fileDescription': Error stream dari 'su cat \"$filePath\"' (exit: $exitCode):\n${errorOutput.toString().trim()}")
                }

                if (exitCode == 0) {
                    val contentSu = output.toString().trim()
                    if (contentSu.isNotBlank()) {
                        return contentSu
                    } else {
                        Log.w(TAG, "'$fileDescription': File kosong (via SU). Path: $filePath")
                        return null
                    }
                } else {
                    Log.e(TAG, "'$fileDescription': Perintah 'su cat \"$filePath\"' gagal dengan exit code $exitCode.")
                }
            } catch (e: IOException) {
                Log.e(TAG, "'$fileDescription': IOException saat menjalankan 'su cat \"$filePath\"'", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "'$fileDescription': InterruptedException saat 'su cat \"$filePath\"'", e)
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "'$fileDescription': Error tidak diketahui saat 'su cat \"$filePath\"'", e)
            } finally {
                process?.destroy()
            }
        }
        return null
    }

    private suspend fun getCpuRealtimeInternal(): RealtimeCpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val governor = readFileToString("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "CPU0 Governor") ?: VALUE_UNKNOWN

        val frequencies = List(cores) { coreIndex ->
            val freqStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq", "CPU$coreIndex Current Freq")
            (freqStr?.toLongOrNull()?.div(1000))?.toInt() ?: 0
        }

        val tempStr = readFileToString("/sys/class/thermal/thermal_zone0/temp", "Thermal Zone0 Temp")
        val temperature = (tempStr?.toFloatOrNull()?.div(1000)) ?: 0f // Asumsi temp dalam mili-Celsius

        val systemInfo = getCachedSystemInfo() // Dapatkan info SoC
        val cpuLoadPercentage = null // Placeholder

        return RealtimeCpuInfo(
            cores = cores,
            governor = governor,
            freqs = frequencies,
            temp = temperature,
            soc = systemInfo.soc, // Menambahkan kembali socModel
            cpuLoadPercentage = cpuLoadPercentage
        )
    }

    fun getCpuRealtime(): RealtimeCpuInfo {
        Log.w(TAG, "Panggilan getCpuRealtime() sinkron. Disarankan menggunakan Flow untuk update realtime.")
        return runBlocking { getCpuRealtimeInternal() }
    }

    private fun getBatteryLevelFromApi(): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatusIntent = context.applicationContext.registerReceiver(null, intentFilter)
        if (batteryStatusIntent == null) {
            Log.w(TAG, "Gagal mendapatkan BatteryStatusIntent.")
            return -1
        }
        val level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level != -1 && scale != -1 && scale != 0) {
            (level / scale.toFloat() * 100).toInt()
        } else -1
    }

    private fun getBatteryInfoInternal(): BatteryInfo {
        val batteryDir = "/sys/class/power_supply/battery"
        val batteryLevelStr = readFileToString("$batteryDir/capacity", "Battery Level Percent from File")
        val finalLevel = batteryLevelStr?.toIntOrNull() ?: getBatteryLevelFromApi().let { if (it == -1) 0 else it }

        var tempStr = readFileToString("$batteryDir/temp", "Battery Temperature")
        var tempSource = "$batteryDir/temp"
        if (tempStr == null) {
            val thermalZoneDirs = File("/sys/class/thermal/").listFiles { dir, name ->
                dir.isDirectory && name.startsWith("thermal_zone")
            }
            thermalZoneDirs?.sortedBy { it.name }?.forEach thermalLoop@{ zoneDir ->
                val type = readFileToString("${zoneDir.path}/type", "Thermal Zone Type (${zoneDir.name})", attemptSu = false)
                if (type != null && (type.contains("battery", ignoreCase = true) || type.contains("แบตเตอรี่") || type.contains("case_therm", ignoreCase = true) || type.contains("ibat_therm", ignoreCase = true))) {
                    tempStr = readFileToString("${zoneDir.path}/temp", "Battery Temperature from ${zoneDir.name} ($type)")
                    if (tempStr != null) {
                        tempSource = "${zoneDir.path}/temp (type: $type)"
                        return@thermalLoop
                    }
                }
            }
        }
        val finalTemperature = tempStr?.toFloatOrNull()?.let { rawTemp ->
            // Jika dari thermal_zone, biasanya dalam mili-Celsius, jika dari power_supply, bisa deci-Celsius
            if (rawTemp > 1000 && (tempSource.contains("thermal_zone") || tempSource.contains("temp_input"))) rawTemp / 1000 else rawTemp / 10
        } ?: 0f

        val cycleCountStr = readFileToString("$batteryDir/cycle_count", "Battery Cycle Count")
        val finalCyclesForInfo = cycleCountStr?.toIntOrNull() ?: 0

        val designCapacityUahStr = readFileToString("$batteryDir/charge_full_design", "Battery Design Capacity (uAh)")
        val designCapacityUah = designCapacityUahStr?.toLongOrNull()
        val finalDesignCapacityMah = if (designCapacityUah != null && designCapacityUah > 0) (designCapacityUah / 1000).toInt() else 0

        var calculatedSohPercentage: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN
        if (finalDesignCapacityMah > 0 && designCapacityUah != null) {
            val currentFullUahStr = readFileToString("$batteryDir/charge_full", "Battery Current Full Capacity (uAh)")
            val currentFullUah = currentFullUahStr?.toLongOrNull()
            if (currentFullUah != null && currentFullUah > 0) {
                val soh = (currentFullUah.toDouble() / designCapacityUah.toDouble()) * 100.0
                calculatedSohPercentage = soh.toInt().coerceIn(0, 100)
            }
        }

        return BatteryInfo(
            level = finalLevel,
            temp = finalTemperature,
            health = calculatedSohPercentage,
            cycles = finalCyclesForInfo,
            capacity = finalDesignCapacityMah
        )
    }

    fun getBatteryInfo(): BatteryInfo {
        Log.w(TAG, "Panggilan getBatteryInfo() sinkron. Disarankan menggunakan Flow untuk update realtime.")
        return getBatteryInfoInternal()
    }

    private fun getMemoryInfoInternal(): MemoryInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            MemoryInfo(
                used = memoryInfo.totalMem - memoryInfo.availMem,
                total = memoryInfo.totalMem,
                free = memoryInfo.availMem
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil MemoryInfo", e)
            MemoryInfo(0, 0, 0)
        }
    }

    fun getMemoryInfo(): MemoryInfo {
        Log.w(TAG, "Panggilan getMemoryInfo() sinkron. Disarankan menggunakan Flow untuk update realtime.")
        return getMemoryInfoInternal()
    }

    private fun getUptimeMillisInternal(): Long {
        return android.os.SystemClock.elapsedRealtime()
    }

    private fun getDeepSleepMillisInternal(): Long {
        val uptime = android.os.SystemClock.elapsedRealtime()
        val awakeTime = android.os.SystemClock.uptimeMillis()
        return uptime - awakeTime
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    fun getDeepSleepInfo(): DeepSleepInfo {
        Log.w(TAG, "Panggilan getDeepSleepInfo() sinkron. Disarankan menggunakan Flow untuk update realtime.")
        return DeepSleepInfo(getUptimeMillisInternal(), getDeepSleepMillisInternal())
    }

    private fun getSystemInfoInternal(): SystemInfo {
        Log.d(TAG, "Mengambil SystemInfo (API based)...")
        var socName = VALUE_UNKNOWN
        try {
            val processManufacturer = Runtime.getRuntime().exec("getprop ro.soc.manufacturer")
            val manufacturer = BufferedReader(InputStreamReader(processManufacturer.inputStream)).readLine()?.trim()
            processManufacturer.waitFor()
            processManufacturer.destroy()

            val processModel = Runtime.getRuntime().exec("getprop ro.soc.model")
            val model = BufferedReader(InputStreamReader(processModel.inputStream)).readLine()?.trim()
            processModel.waitFor()
            processModel.destroy()

            if (!manufacturer.isNullOrBlank() && !model.isNullOrBlank()) {
                socName = when {
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM7475", ignoreCase = true) -> "Qualcomm® Snapdragon™ 7+ Gen 2"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8650", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Gen 3"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8635", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8s Gen 3"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM845", ignoreCase = true) || model.equals("sdm845", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 845"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8250", ignoreCase = true) -> "Qualcomm® Snapdragon™ 870"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8150", ignoreCase = true) -> "Qualcomm® Snapdragon™ 860"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM7435-AB", ignoreCase = true) || model.equals("SM7435", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 7s Gen 2"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM8735", ignoreCase = true) || model.equals("sm8735", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 8s Gen 4"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM665", ignoreCase = true) || model.equals("sdm665", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 665"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM660", ignoreCase = true) || model.equals("sdm660", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 660"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8750", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Elite"
                    manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6785V/CD", ignoreCase = true) || model.equals("MT6785", ignoreCase = true)) -> "MediaTek Helio G95"
                    manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6877V/TTZA", ignoreCase = true) || model.equals("MT6877V", ignoreCase = true)) -> "MediaTek Dimensity 1080"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6833GP", ignoreCase = true) -> "MediaTek Dimensity 6080"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6769Z", ignoreCase = true) -> "MediaTek Helio G85"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6989W", ignoreCase = true) -> "MediaTek Dimensity 9300+"
                    else -> "$manufacturer $model"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mendapatkan info SOC dari getprop", e)
        }

        return SystemInfo(
            model = android.os.Build.MODEL ?: VALUE_UNKNOWN,
            codename = android.os.Build.DEVICE ?: VALUE_UNKNOWN,
            androidVersion = android.os.Build.VERSION.RELEASE ?: VALUE_UNKNOWN,
            sdk = android.os.Build.VERSION.SDK_INT,
            fingerprint = android.os.Build.FINGERPRINT ?: VALUE_UNKNOWN,
            soc = socName
        )
    }

    fun getSystemInfo(): SystemInfo {
        return runBlocking { getCachedSystemInfo() }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val realtimeAggregatedInfoFlow: Flow<RealtimeAggregatedInfo> = callbackFlow {
        Log.d(TAG, "callbackFlow started for realtimeAggregatedInfoFlow")

        // Dapatkan SystemInfo sekali di awal (terutama untuk SoC Name)
        // Ini akan mengisi cache jika belum ada
        // getCachedSystemInfo() adalah suspend, jadi panggil dalam konteks coroutine jika perlu
        // Namun, callbackFlow sudah berjalan dalam konteks coroutine.
        getCachedSystemInfo() // Memastikan cache terisi

        // Kirim nilai awal segera
        val initialData = RealtimeAggregatedInfo(
            cpuInfo = getCpuRealtimeInternal(), // Akan menggunakan cache jika socModel diperlukan
            batteryInfo = getBatteryInfoInternal(),
            memoryInfo = getMemoryInfoInternal(),
            uptimeMillis = getUptimeMillisInternal(),
            deepSleepMillis = getDeepSleepMillisInternal()
        )

        // Menggunakan trySend yang mengembalikan ChannelResult
        val initialSendResult: ChannelResult<Unit> = trySend(initialData)
        if (initialSendResult.isFailure) {
            Log.e(TAG, "Failed to send initial data to flow", initialSendResult.exceptionOrNull())
        } else if (initialSendResult.isClosed) {
            Log.w(TAG, "Flow was closed before initial data could be sent.")
        } else {
            Log.d(TAG, "Initial data sent successfully to flow.")
        }

        // job untuk update periodik
        val updateJob = launch(Dispatchers.IO) { // Gunakan Dispatchers.IO untuk delay dan I/O
            Log.d(TAG, "Realtime update job started in callbackFlow. isActive: $isActive")
            try {
                while (isActive) { // Loop selama Flow (dan job ini) aktif
                    delay(REALTIME_UPDATE_INTERVAL_MS)
                    // Tidak perlu cek isActive lagi di sini karena delay akan throw CancellationException
                    // jika scope atau job di-cancel.

                    // Log.d(TAG, "Preparing to send updated realtime data...")
                    val updatedData = RealtimeAggregatedInfo(
                        cpuInfo = getCpuRealtimeInternal(),
                        batteryInfo = getBatteryInfoInternal(),
                        memoryInfo = getMemoryInfoInternal(),
                        uptimeMillis = getUptimeMillisInternal(),
                        deepSleepMillis = getDeepSleepMillisInternal()
                    )
                    val sendResult: ChannelResult<Unit> = trySend(updatedData)

                    when {
                        sendResult.isSuccess -> { /* Log.d(TAG, "Realtime data sent successfully.") */ }
                        sendResult.isClosed -> {
                            Log.w(TAG, "Flow closed while trying to send realtime data. Loop will terminate.")
                            break // Keluar dari loop jika channel ditutup
                        }
                        sendResult.isFailure -> {
                            Log.e(TAG, "Error sending realtime data to flow", sendResult.exceptionOrNull())
                            // Pertimbangkan apa yang harus dilakukan jika terjadi error. Mungkin coba lagi atau hentikan.
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Realtime update job cancelled: ${e.message}")
                // Ini normal jika flow ditutup
            } finally {
                Log.d(TAG, "Realtime update job finished. isActive: $isActive")
            }
        }

        // awaitClose akan dipanggil ketika flow di-cancel atau scope-nya di-cancel
        awaitClose {
            Log.d(TAG, "RealtimeAggregatedInfoFlow (awaitClose) triggered, cancelling update job.")
            updateJob.cancel("Flow was closed") // Batalkan job yang melakukan update periodik
            Log.d(TAG, "Realtime update job cancellation requested.")
        }
    }.shareIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L, replayExpirationMillis = 0L),
        replay = 1
    )

    fun onCleared() {
        Log.d(TAG, "SystemRepository onCleared, cancelling repositoryScope.")
        repositoryScope.cancel("Repository is being cleared")
    }

    fun getKernelInfo(): KernelInfo {
        Log.d(TAG, "Memulai pengambilan KernelInfo...")
        val rawKernelVersionOutput = readFileToString("/proc/version", "Full Kernel Version String")

        val parsedVersion = if (!rawKernelVersionOutput.isNullOrBlank()) {
            rawKernelVersionOutput.substringBefore("\n").trim().ifEmpty { VALUE_NOT_AVAILABLE }
        } else {
            VALUE_NOT_AVAILABLE
        }
        Log.d(TAG, "Versi Kernel (baris pertama terparsir): $parsedVersion")

        val gkiType: String
        if (rawKernelVersionOutput == null || parsedVersion == VALUE_NOT_AVAILABLE) {
            gkiType = VALUE_NOT_AVAILABLE
            Log.w(TAG, "Tidak bisa menentukan GKI Type karena rawKernelVersionOutput null atau versi tidak tersedia.")
        } else {
            val kernelVersionRegex = "Linux version (\\d+\\.\\d+)".toRegex()
            val matchResult = kernelVersionRegex.find(rawKernelVersionOutput)
            val linuxKernelBaseVersion = matchResult?.groups?.get(1)?.value
            Log.d(TAG, "Ekstraksi Versi Linux Kernel dari raw: $linuxKernelBaseVersion (Full raw: '$rawKernelVersionOutput')")

            gkiType = when (linuxKernelBaseVersion) {
                "6.1" -> "GKI 2.0 (6.1)"
                "5.15" -> "GKI 2.0 (5.15)"
                "5.10" -> "GKI 2.0 (5.10)"
                "5.4" -> "GKI 2.0 (5.4)"
                "4.19" -> "GKI 1.0 (4.19)"
                "4.14" -> "GKI 1.0 (4.14)"
                "4.9" -> "EAS (4.9)"
                else -> {
                    when {
                        rawKernelVersionOutput.contains("android14-") -> "GKI 2.0 (Android 14 based)"
                        rawKernelVersionOutput.contains("android13-") -> "GKI 2.0 (Android 13 based)"
                        rawKernelVersionOutput.contains("android12-") -> "GKI 2.0 (Android 12 based)"
                        rawKernelVersionOutput.contains("android11-") -> "GKI 1.0 (Android 11 based)"
                        rawKernelVersionOutput.contains("perf-") -> "EAS (Perf-based)"
                        else -> {
                            Log.d(TAG, "Tidak ada pola GKI yang cocok untuk versi Linux '$linuxKernelBaseVersion' atau string 'androidXX-'. Menganggap Non-GKI atau Unknown.")
                            if (linuxKernelBaseVersion != null) "Non-GKI ($linuxKernelBaseVersion)" else VALUE_UNKNOWN
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Tipe GKI Terdeteksi: $gkiType")

        val schedulerPath = "/sys/block/sda/queue/scheduler"
        val rawSchedulerOutput = readFileToString(schedulerPath, "I/O Scheduler String")
        var parsedSchedulerName = if (rawSchedulerOutput != null && rawSchedulerOutput != "0" && rawSchedulerOutput.isNotBlank()) {
            rawSchedulerOutput.substringAfterLast("[").substringBefore("]").trim()
        } else {
            val altSchedulerPath = "/sys/block/mmcblk0/queue/scheduler"
            val altRawScheduler = readFileToString(altSchedulerPath, "Alt I/O Scheduler (mmcblk0)")
            if (altRawScheduler != null && altRawScheduler != "0" && altRawScheduler.isNotBlank()) {
                altRawScheduler.substringAfterLast("[").substringBefore("]").trim()
            } else {
                VALUE_NOT_AVAILABLE
            }
        }
        if (parsedSchedulerName.isEmpty()) parsedSchedulerName = VALUE_NOT_AVAILABLE

        val parsedScheduler = when (parsedSchedulerName.lowercase()) {
            "bfq" -> "BFQ (Budget Fair Queueing)"
            "cfq" -> "CFQ (Completely Fair Queuing)"
            else -> if (parsedSchedulerName != VALUE_NOT_AVAILABLE) parsedSchedulerName else VALUE_NOT_AVAILABLE
        }
        Log.d(TAG, "Scheduler I/O Terparsir: $parsedScheduler (mentah utama: '$rawSchedulerOutput')")

        return KernelInfo(
            version = parsedVersion,
            gkiType = gkiType,
            scheduler = parsedScheduler
        )
    }

    fun getCpuClusters(): List<CpuCluster> {
        Log.d(TAG, "Memulai pengambilan CPU Clusters...")
        val cpuPolicyDir = File("/sys/devices/system/cpu/policy")
        val clusters = mutableListOf<CpuCluster>()

        if (!cpuPolicyDir.exists() || !cpuPolicyDir.isDirectory) {
            Log.w(TAG, "Direktori CPU policy tidak ditemukan: ${cpuPolicyDir.path}")
            return emptyList()
        }

        cpuPolicyDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("policy") && it.name.length > "policy".length && it.name.substring("policy".length).toIntOrNull() != null }
            ?.sortedBy { it.name.removePrefix("policy").toInt() }
            ?.forEachIndexed { index, policyFile ->
                val policyNum = policyFile.name.removePrefix("policy")
                val policyDesc = "Policy$policyNum"
                // Log.d(TAG, "Memproses CPU Cluster: $policyDesc (${policyFile.path})") // Optional: kurangi log spam

                val relatedCpus = readFileToString("${policyFile.path}/related_cpus", "$policyDesc Related CPUs", attemptSu = false)?.trim()
                val firstCpuInCluster = relatedCpus?.split(" ")?.firstOrNull()?.toIntOrNull()

                val clusterNameSuggestion = when (index) { // Penamaan cluster bisa lebih kompleks tergantung arsitektur
                    0 -> "Little"
                    1 -> if (cpuPolicyDir.listFiles()?.count { it.name.startsWith("policy") } == 2) "Big" else "Mid" // Contoh sederhana
                    2 -> "Big" // Atau Prime
                    else -> "Cluster ${index + 1}"
                }
                val clusterName = "$clusterNameSuggestion" + (if (firstCpuInCluster != null) " (CPU$firstCpuInCluster+)" else " ($policyDesc)")


                val governor = readFileToString("${policyFile.path}/scaling_governor", "$policyDesc Governor") ?: VALUE_UNKNOWN
                val minFreqStr = readFileToString("${policyFile.path}/cpuinfo_min_freq", "$policyDesc Min Freq")
                val minFreq = minFreqStr?.toIntOrNull()?.div(1000) ?: 0
                val maxFreqStr = readFileToString("${policyFile.path}/cpuinfo_max_freq", "$policyDesc Max Freq")
                val maxFreq = maxFreqStr?.toIntOrNull()?.div(1000) ?: 0

                val availableGovernorsStr = readFileToString("${policyFile.path}/scaling_available_governors", "$policyDesc Available Governors")
                val availableGovernors = availableGovernorsStr?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()

                clusters.add(CpuCluster(clusterName, minFreq, maxFreq, governor, availableGovernors))
                // Log.d(TAG, "Cluster '$clusterName': Gov=$governor, Min=${minFreq}MHz, Max=${maxFreq}MHz, AvailGov=$availableGovernors")
            }

        if (clusters.isEmpty()) {
            Log.w(TAG, "Tidak ada CPU cluster yang terdeteksi atau dapat diproses di ${cpuPolicyDir.path}")
        }
        // Log.i(TAG, "CPU Clusters hasil akhir: $clusters") // Optional: kurangi log spam
        return clusters
    }
}
