package id.xms.xtrakernelmanager.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpuSysfsScanner @Inject constructor() {

    /* GPU busy + clk (kgsl, mali, adreno, dimensity) */
    val GPU_PAIRS = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpubusy" to "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/mali0/device/gpu_busy" to "/sys/class/mali0/device/gpu_clock",
        "/sys/devices/platform/13000000.mali/gpu_busy" to "/sys/devices/platform/13000000.mali/gpu_clock",
        "/sys/kernel/gpu/gpu_busy" to "/sys/kernel/gpu/gpu_clock",
        "/sys/class/devfreq/1c40000.gpu/load" to "/sys/class/devfreq/1c40000.gpu/cur_freq",
        "/sys/class/devfreq/gpufreq/load" to "/sys/class/devfreq/gpufreq/cur_freq"
    )

    /* DRM measured_fps */
    val DRM_NODES = listOf(
        "/sys/class/drm/sde-crtc-0/measured_fps",
        "/sys/class/graphics/fb0/measured_fps",
        "/sys/class/drm/card0/card0-DSI-1/measured_fps",
        "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/measured_fps",
        "/sys/kernel/msm_drm/measure_fps",
        "/sys/module/msm_drm/parameters/measured_fps"
    )

    /* ---------- helper ---------- */
    fun firstExisting(vararg paths: String): String? =
        paths.firstOrNull { RootUtils.runCommandAsRoot("[[ -e $it ]] && echo 1").equals("1") }

    fun readInt(path: String): Int? =
        RootUtils.runCommandAsRoot("cat $path")?.trim()?.toIntOrNull()
}