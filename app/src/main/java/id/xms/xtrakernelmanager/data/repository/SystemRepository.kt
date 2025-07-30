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
        if (currentTime - lastCpuRealtimeUpdate < 5000 && cachedCpuRealtimeInfo != null) {
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

        val level = safeRead("$dir/capacity", "-1").toIntOrNull()
            ?: getBatteryLevelFromApi()

        val temp = safeRead("$dir/temp", "0").toFloatOrNull()?.div(10) ?: 0f

        val health = safeRead("$dir/health", "0").toIntOrNull()
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        val cycles = safeRead("$dir/cycle_count", "0").toIntOrNull() ?: 0

        val capacity = safeRead("$dir/charge_full_design", "0").toIntOrNull()
            ?.div(1000) ?: 0

        return BatteryInfo(level, temp, health, cycles, capacity)
    }

    private fun getBatteryLevelFromApi(): Int {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    /* ---------- CPU clusters ---------- */
    fun getCpuClusters(): List<CpuCluster> {
        val list = mutableListOf<CpuCluster>()
        File("/sys/devices/system/cpu/policy")
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEachIndexed { idx, f ->
                val name = when (idx) {
                    0 -> "Little"
                    1 -> "Big"
                    2 -> "Prime"
                    else -> "Cluster$idx"
                }
                val gov = safeRead("${f.path}/scaling_governor", "unknown")
                val min = safeRead("${f.path}/cpuinfo_min_freq", "0").toIntOrNull() ?: 0
                val max = safeRead("${f.path}/cpuinfo_max_freq", "0").toIntOrNull() ?: 0
                val avGov = safeRead("${f.path}/scaling_available_governors", "")
                    .split(" ").filter { it.isNotBlank() }
                list += CpuCluster(name, min, max, gov, avGov)
            }
        return list
    }

    /* ---------- System info ---------- */
    fun getSystemInfo(): SystemInfo = SystemInfo(
        model = android.os.Build.MODEL,
        codename = android.os.Build.DEVICE,
        androidVersion = android.os.Build.VERSION.RELEASE,
        sdk = android.os.Build.VERSION.SDK_INT,
        buildNumber = android.os.Build.DISPLAY
    )

    /* ---------- Kernel info ---------- */
    fun getKernelInfo(): KernelInfo {
        val ver = safeRead("/proc/version")
        return KernelInfo(
            version = ver.substringBefore("\n").trim(),
            gkiType = if (ver.contains("android12-")) "GKI 2.0" else "GKI 1.0",
            scheduler = safeRead("/sys/block/sda/queue/scheduler")
                .substringAfter("[")
                .substringBefore("]")
                .trim()
        )
    }

    /* ---------- GPU info ---------- */
    fun getGpuInfo(): GpuInfo {
        val dir = "/sys/class/kgsl/kgsl-3d0/devfreq"
        return GpuInfo(
            renderer = getProp("ro.hardware.egl", "Unknown"),
            glEsVersion = getProp("ro.opengles.version", "3.2"),
            governor = safeRead("$dir/governor", "unknown"),
            availableGovernors = safeRead("$dir/available_governors", "")
                .split(" ").filter { it.isNotBlank() },
            minFreq = safeRead("$dir/min_freq", "0").toIntOrNull() ?: 0,
            maxFreq = safeRead("$dir/max_freq", "0").toIntOrNull() ?: 0
        )
    }

    /* ---------- Reflection helper ---------- */
    @Suppress("DiscouragedPrivateApi")
    private fun getProp(key: String, default: String = ""): String =
        runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java, String::class.java)
                .invoke(null, key, default) as String
        }.getOrDefault(default)
}