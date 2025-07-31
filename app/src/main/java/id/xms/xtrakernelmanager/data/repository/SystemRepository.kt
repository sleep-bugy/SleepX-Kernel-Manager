package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.vector.path
import id.xms.xtrakernelmanager.data.model.*
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
    private val context: Context
) {

    companion object {
        private const val TAG = "SystemRepository"
        private const val VALUE_NOT_AVAILABLE = "N/A"
        private const val VALUE_UNKNOWN = "Unknown"
        private const val TARGET_CYCLES_FOR_80_PERCENT_HEALTH = 500
        private const val TARGET_HEALTH_AT_TARGET_CYCLES = 80
        // Persentase kehilangan per siklus
        private const val DEGRADATION_PERCENT_PER_CYCLE =
            (100.0 - TARGET_HEALTH_AT_TARGET_CYCLES) / TARGET_CYCLES_FOR_80_PERCENT_HEALTH
    }

    // --- Fungsi Helper Inti untuk Membaca File dengan Fallback 'su' ---
    private fun readFileToString(filePath: String, fileDescription: String, attemptSu: Boolean = true): String? {
        val file = File(filePath)
        Log.d(TAG, "Membaca '$fileDescription' dari: $filePath (Attempt SU: $attemptSu)")
        try {
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                if (content.isNotBlank()) {
                    Log.d(TAG, "'$fileDescription': Konten mentah (langsung) = '$content'")
                    return content
                } else {
                    Log.w(TAG, "'$fileDescription': File kosong (langsung). Path: $filePath")
                    if (!attemptSu) return null
                }
            } else {
                Log.w(TAG, "'$fileDescription': File tidak ada/baca (langsung). Path: $filePath. Exists: ${file.exists()}, CanRead: ${file.canRead()}")
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
            Log.i(TAG, "'$fileDescription': Mencoba membaca $filePath menggunakan 'su cat'")
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat \"$filePath\""))
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
                        Log.i(TAG, "'$fileDescription': Konten mentah (via SU) = '$contentSu'")
                        return contentSu
                    } else {
                        Log.w(TAG, "'$fileDescription': File kosong (via SU). Path: $filePath")
                        return null
                    }
                } else {
                    Log.e(TAG, "'$fileDescription': Perintah 'su cat \"$filePath\"' gagal dengan exit code $exitCode.")
                    if (output.isNotBlank()) {
                        Log.w(TAG, "'$fileDescription': Output stdout dari 'su cat' saat gagal:\n${output.toString().trim()}")
                    }
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
        } else {
            Log.d(TAG, "'$fileDescription': Tidak mencoba membaca $filePath via SU (attemptSu false atau baca langsung gagal tanpa SecurityException).")
        }

        Log.e(TAG, "'$fileDescription': GAGAL membaca dari $filePath setelah semua percobaan.")
        return null
    }

    private var lastCpuRealtimeUpdate = 0L
    private var cachedCpuRealtimeInfo: RealtimeCpuInfo? = null

    fun getCpuRealtime(): RealtimeCpuInfo {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCpuRealtimeUpdate < 2000 && cachedCpuRealtimeInfo != null) {
            return cachedCpuRealtimeInfo!!
        }
        Log.d(TAG, "Memperbarui RealtimeCpuInfo...")

        val cores = Runtime.getRuntime().availableProcessors()
        val governor = readFileToString("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "CPU0 Governor") ?: VALUE_UNKNOWN

        val frequencies = List(cores) { coreIndex ->
            val freqStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq", "CPU$coreIndex Current Freq")
            (freqStr?.toLongOrNull()?.div(1000))?.toInt() ?: 0
        }

        val tempStr = readFileToString("/sys/class/thermal/thermal_zone0/temp", "Thermal Zone0 Temp")
        val temperature = (tempStr?.toFloatOrNull()?.div(1000)) ?: 0f

        cachedCpuRealtimeInfo = RealtimeCpuInfo(cores, governor, frequencies, temperature)
        lastCpuRealtimeUpdate = currentTime
        Log.d(TAG, "RealtimeCpuInfo diperbarui: $cachedCpuRealtimeInfo")
        return cachedCpuRealtimeInfo!!
    }


    // --- Battery Info ---
    private fun getBatteryLevelFromApi(): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatusIntent = context.registerReceiver(null, intentFilter)

        if (batteryStatusIntent == null) {
            Log.w(TAG, "Gagal mendapatkan BatteryStatusIntent (context bermasalah?).")
            return -1
        }

        val level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        return if (level != -1 && scale != -1 && scale != 0) {
            val batteryPct = (level / scale.toFloat() * 100).toInt()
            Log.d(TAG, "Level baterai dari API: $batteryPct%")
            batteryPct
        } else {
            Log.w(TAG, "Gagal mendapatkan level baterai valid dari API: level=$level, scale=$scale")
            -1
        }
    }
    fun getBatteryInfo(): BatteryInfo {
        Log.d(TAG, "Memulai pengambilan BatteryInfo (struktur BatteryInfo.kt dipertahankan)...")
        val batteryDir = "/sys/class/power_supply/battery"
        val batteryLevelStr = readFileToString("$batteryDir/capacity", "Battery Level Percent from File")
        val currentLevelFromFile = batteryLevelStr?.toIntOrNull()
        val finalLevel = currentLevelFromFile ?: run {
            Log.i(TAG, "Level baterai dari file tidak valid, menggunakan API.")
            getBatteryLevelFromApi()
        }.let { if (it == -1) 0 else it }
        Log.d(TAG, "Final Level Baterai (untuk BatteryInfo.level): $finalLevel%")

        var tempStr = readFileToString("$batteryDir/temp", "Battery Temperature")
        var tempSource = "$batteryDir/temp"
        if (tempStr == null) {
            Log.w(TAG, "Gagal baca suhu dari '$tempSource', mencoba path alternatif...")
            val thermalZoneDirs = File("/sys/class/thermal/").listFiles { dir, name ->
                dir.isDirectory && name.startsWith("thermal_zone")
            }
            var foundTempInThermalZone = false
            thermalZoneDirs?.sortedBy { it.name }?.forEach { zoneDir ->
                val type = readFileToString("${zoneDir.path}/type", "Thermal Zone Type (${zoneDir.name})", attemptSu = false)
                if (type != null && (type.contains("battery", ignoreCase = true) || type.contains("แบตเตอรี่") || type.contains("case_therm", ignoreCase = true) || type.contains("ibat_therm", ignoreCase = true))) {
                    tempStr = readFileToString("${zoneDir.path}/temp", "Battery Temperature from ${zoneDir.name} ($type)")
                    if (tempStr != null) {
                        tempSource = "${zoneDir.path}/temp (type: $type)"
                        foundTempInThermalZone = true
                        return@forEach
                    }
                }
            }
            if (!foundTempInThermalZone) {
                Log.w(TAG, "Tidak menemukan file suhu baterai yang valid di thermal_zones.")
            }
        }
        val finalTemperature = tempStr?.toFloatOrNull()?.let { rawTemp ->
            if (tempSource.startsWith("/sys/class/thermal/thermal_zone")) rawTemp / 1000 else rawTemp / 10
        } ?: 0f
        Log.d(TAG, "Final Suhu Baterai (untuk BatteryInfo.temp): $finalTemperature°C (mentah: '$tempStr' dari $tempSource)")


        val cycleCountStr = readFileToString("$batteryDir/cycle_count", "Battery Cycle Count")
        val finalCyclesForInfo = cycleCountStr?.toIntOrNull() ?: 0 // 0 jika tidak tersedia
        Log.d(TAG, "Jumlah siklus baterai (untuk BatteryInfo.cycles): $finalCyclesForInfo (mentah: '$cycleCountStr')")

        val designCapacityUahStr = readFileToString("$batteryDir/charge_full_design", "Battery Design Capacity (uAh)")
        val designCapacityUah = designCapacityUahStr?.toLongOrNull()
        val finalDesignCapacityMah = if (designCapacityUah != null && designCapacityUah > 0) (designCapacityUah / 1000).toInt() else 0
        if (finalDesignCapacityMah == 0) {
            Log.w(TAG, "Kapasitas desain ('charge_full_design') tidak ditemukan atau tidak valid. BatteryInfo.capacity akan 0. Perhitungan kesehatan SoH tidak mungkin.")
        }
        Log.d(TAG, "Desain kapasitas (untuk BatteryInfo.capacity): $finalDesignCapacityMah mAh (dari uAh: $designCapacityUah, mentah: '$designCapacityUahStr')")


        var calculatedSohPercentage: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN
        if (finalDesignCapacityMah > 0) {
            var currentFullUahStr = readFileToString("$batteryDir/charge_full", "Battery Current Full Capacity (uAh)")
            var currentFullUahSource = "$batteryDir/charge_full"

            if (currentFullUahStr == null) {
                Log.w(TAG, "File '$currentFullUahSource' tidak ditemukan, SoH mungkin tidak akurat atau menggunakan fallback.")
            }

            val currentFullUah = currentFullUahStr?.toLongOrNull()

            if (currentFullUah != null && currentFullUah > 0) {
                val currentFullMah = (currentFullUah / 1000).toInt()
                Log.i(TAG, "Kapasitas Penuh Saat Ini (dari '$currentFullUahSource'): $currentFullMah mAh (mentah uAh: $currentFullUah)")

                val soh = (currentFullUah.toDouble() / designCapacityUah!!.toDouble()) * 100.0
                calculatedSohPercentage = soh.toInt().coerceIn(0, 100)
                Log.i(TAG, "Estimasi Kesehatan Baterai (SoH dari kapasitas untuk BatteryInfo.health): $calculatedSohPercentage% (Current: $currentFullMah mAh, Design: $finalDesignCapacityMah mAh)")
            } else {
                Log.w(TAG, "Tidak dapat membaca kapasitas penuh saat ini ('$currentFullUahSource' atau fallback) atau nilainya tidak valid. SoH tidak dapat dihitung dengan metode kapasitas.")
            }
        } else {
            Log.e(TAG, "Kapasitas desain adalah 0, tidak mungkin menghitung SoH. BatteryInfo.health akan default.")
        }

        val qualitativeHealthString = readFileToString("$batteryDir/health", "Battery Qualitative Health String")
        val result = BatteryInfo(
            level = finalLevel,
            temp = finalTemperature,
            health = calculatedSohPercentage,
            cycles = finalCyclesForInfo,
            capacity = finalDesignCapacityMah
        )
        Log.i(TAG, "BatteryInfo hasil akhir (struktur dipertahankan): $result")
        return result
    }

    fun getMemoryInfo(): MemoryInfo {
        Log.d(TAG, "Mengambil MemoryInfo...")
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            MemoryInfo(
                used = memoryInfo.totalMem - memoryInfo.availMem,
                total = memoryInfo.totalMem,
                free = memoryInfo.availMem
            ).also { Log.d(TAG, "MemoryInfo: $it") }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil MemoryInfo", e)
            MemoryInfo(0, 0, 0)
        }
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
                        rawKernelVersionOutput.contains("perf-")-> "EAS (Perf-based)"
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
            "bfq" -> {
                "BFQ (Budget Fair Queueing)"
            }
            "cfq" -> {
                "CFQ (Completely Fair Queuing)"
            }
            else ->
                if (parsedSchedulerName != VALUE_NOT_AVAILABLE) parsedSchedulerName else VALUE_NOT_AVAILABLE

        }
        Log.d(TAG, "Scheduler I/O Terparsir: $parsedScheduler (mentah utama: '$rawSchedulerOutput')")

        val result = KernelInfo(
            version = parsedVersion,
            gkiType = gkiType,
            scheduler = parsedScheduler
        )
        Log.i(TAG, "KernelInfo hasil akhir: $result")
        return result
    }

    fun getDeepSleepInfo(): DeepSleepInfo {
        Log.d(TAG, "Mengambil DeepSleepInfo...")
        val uptime = android.os.SystemClock.elapsedRealtime()
        val awakeTime = android.os.SystemClock.uptimeMillis()
        val deepSleepTime = uptime - awakeTime
        return DeepSleepInfo(uptime, deepSleepTime).also { Log.d(TAG, "DeepSleepInfo: $it") }
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
                Log.d(TAG, "Memproses CPU Cluster: $policyDesc (${policyFile.path})")

                val relatedCpus = readFileToString("${policyFile.path}/related_cpus", "$policyDesc Related CPUs", attemptSu = false)?.trim()
                val firstCpuInCluster = relatedCpus?.split(" ")?.firstOrNull()?.toIntOrNull()

                val clusterName = when (index) {
                    0 -> "Little"
                    1 -> "Big"
                    2 -> "Prime"
                    else -> "Cluster ${index + 1}"
                } + (if (firstCpuInCluster != null) " (CPU$firstCpuInCluster+)" else " ($policyDesc)")

                val governor = readFileToString("${policyFile.path}/scaling_governor", "$policyDesc Governor") ?: VALUE_UNKNOWN
                val minFreqStr = readFileToString("${policyFile.path}/cpuinfo_min_freq", "$policyDesc Min Freq")
                val minFreq = minFreqStr?.toIntOrNull()?.div(1000) ?: 0
                val maxFreqStr = readFileToString("${policyFile.path}/cpuinfo_max_freq", "$policyDesc Max Freq")
                val maxFreq = maxFreqStr?.toIntOrNull()?.div(1000) ?: 0

                val availableGovernorsStr = readFileToString("${policyFile.path}/scaling_available_governors", "$policyDesc Available Governors")
                val availableGovernors = availableGovernorsStr?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()

                clusters.add(CpuCluster(clusterName, minFreq, maxFreq, governor, availableGovernors))
                Log.d(TAG, "Cluster '$clusterName': Gov=$governor, Min=${minFreq}MHz, Max=${maxFreq}MHz, AvailGov=$availableGovernors")
            }

        if (clusters.isEmpty()) {
            Log.w(TAG, "Tidak ada CPU cluster yang terdeteksi atau dapat diproses di ${cpuPolicyDir.path}")
        }
        Log.i(TAG, "CPU Clusters hasil akhir: $clusters")
        return clusters
    }

    fun getSystemInfo(): SystemInfo {
        Log.d(TAG, "Mengambil SystemInfo (API based)...")
        return SystemInfo(
            model = android.os.Build.MODEL,
            codename = android.os.Build.DEVICE,
            androidVersion = android.os.Build.VERSION.RELEASE,
            sdk = android.os.Build.VERSION.SDK_INT,
            buildNumber = android.os.Build.DISPLAY
        ).also { Log.d(TAG, "SystemInfo: $it") }
    }
}