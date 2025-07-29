package id.xms.xtrakernelmanager.util

import java.io.File

object SoCUtils {
    fun detectSoC(): String =
        File("/proc/device-tree/compatible").readText()
            .split('\u0000')
            .lastOrNull { it.isNotBlank() }
            ?.substringAfterLast(",") ?: "Unknown"
}