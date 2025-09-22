package id.xms.xtrakernelmanager.data.repository

import id.xms.xtrakernelmanager.util.GpuSysfsScanner
import id.xms.xtrakernelmanager.util.RootUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpuFpsRepository @Inject constructor(
    private val scanner: GpuSysfsScanner
) {

    /* hasil scan disimpan sekali saat init */
    private val gpuBusyPath: String?
    private val gpuClkPath: String?
    private val drmPath: String?

    init {
        val (busy, clk) = scanner.GPU_PAIRS.firstNotNullOfOrNull { (busy, clk) ->
            if (scanner.firstExisting(busy, clk) != null) busy to clk else null
        } ?: (null to null)
        gpuBusyPath = busy
        gpuClkPath = clk
        drmPath = scanner.firstExisting(*scanner.DRM_NODES.toTypedArray())
    }

    /* -------------- GPU -------------- */
    fun gpuBusy(): Pair<Int, Int>? {
        val raw = gpuBusyPath?.let { RootUtils.runCommandAsRoot("cat $it") } ?: return null
        val nums = raw.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        return if (nums.size == 2) nums[0] to nums[1] else null
    }

    fun gpuClkMHz(): Int =
        gpuClkPath?.let { scanner.readInt(it)?.div(1_000_000) } ?: 0

    fun pixelOsFps(): Int =
        RootUtils.runCommandAsRoot("cat /sys/class/drm/sde-crtc-0/measured_fps")
            ?.substringAfter("fps:")
            ?.substringBefore(" ")
            ?.trim()
            ?.toFloatOrNull()?.toInt() ?: 0

    /* -------------- DRM -------------- */
    fun drmFps(): Int {
        return scanner.readInt(drmPath ?: return -1) ?: -1
    }
    /* -------------- estimasi ---------- */
    fun estimateFps(): Int {
        val (usage, total) = gpuBusy() ?: return -1
        val load = if (total > 0) usage * 100 / total else 0
        val freq = gpuClkMHz()
        val hz = drmFps().coerceAtLeast(60)

        val candidat = when {
            load < 5 -> 0
            freq >= 580 && load > 55 -> 120
            freq >= 450 && load > 45 -> 90
            freq >= 320 && load > 35 -> 60
            else -> 60
        }
        return candidat.coerceAtMost(hz)
    }
}