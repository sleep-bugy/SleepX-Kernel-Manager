package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import id.xms.xtrakernelmanager.data.model.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor(
    private val context: Context
) {

    /* ---------- Helper safe read ---------- */
    private fun safeRead(path: String, default: String = "0"): String =
        runCatching { File(path).readText().trim() }.getOrDefault(default)

    /* ---------- Battery (fallback ke API jika sysfs gagal) ---------- */
    fun getBatteryInfo(): BatteryInfo {
        val dir = "/sys/class/power_supply/battery"

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