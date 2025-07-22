package id.xms.xtrakernelmanager.data.repository

import android.util.Log
import id.xms.xtrakernelmanager.util.RootUtils
import java.io.BufferedReader
import java.io.InputStreamReader

object SystemRepository {
    fun getKernelVersion(): String {
        if (!RootUtils.isDeviceRooted()) return "N/A"
        try {
            val process = Runtime.getRuntime().exec("su")
            val output = BufferedReader(InputStreamReader(process.inputStream))
            process.outputStream.use { it.write("cat /proc/version\n".toByteArray()) }
            process.outputStream.flush()
            val result = output.readLine()?.split(" ")?.getOrNull(2) ?: "N/A"
            Log.d("SystemRepository", "Kernel version: $result")
            return result
        } catch (e: Exception) {
            Log.e("SystemRepository", "Error getting kernel version: ${e.message}")
            return "N/A"
        }
    }

    fun getUsedRam(): String {
        if (!RootUtils.isDeviceRooted()) {
            Log.d("SystemRepository", "Not rooted, returning N/A")
            return "N/A"
        }
        try {
            val process = Runtime.getRuntime().exec("su")
            val output = BufferedReader(InputStreamReader(process.inputStream))
            process.outputStream.use { it.write("cat /proc/meminfo\n".toByteArray()) }
            process.outputStream.flush()

            var totalRam = 0L
            var availableRam = 0L
            output.useLines { lines ->
                lines.forEach { line ->
                    Log.d("SystemRepository", "Processing line: $line")
                    when {
                        line.startsWith("MemTotal:") -> {
                            val rawValue = line.substringAfter("MemTotal:").trim()
                            Log.d("SystemRepository", "Raw MemTotal value: '$rawValue'")
                            totalRam = rawValue.split(" ")[0].toLongOrNull() ?: 0L
                            Log.d("SystemRepository", "Parsed MemTotal: $totalRam KB")
                        }
                        line.startsWith("MemAvailable:") -> {
                            val rawValue = line.substringAfter("MemAvailable:").trim()
                            Log.d("SystemRepository", "Raw MemAvailable value: '$rawValue'")
                            availableRam = rawValue.split(" ")[0].toLongOrNull() ?: 0L
                            Log.d("SystemRepository", "Parsed MemAvailable: $availableRam KB")
                        }
                    }
                }
            }
            Log.d("SystemRepository", "Total: $totalRam KB, Available: $availableRam KB")
            if (totalRam > 0 && availableRam > 0) {
                val usedRam = (totalRam - availableRam) / 1024 // Konversi ke MB
                Log.d("SystemRepository", "Used RAM: $usedRam MB")
                return "${usedRam} MB"
            } else {
                Log.d("SystemRepository", "Invalid RAM data, returning N/A. Total: $totalRam, Available: $availableRam")
                return "N/A"
            }
        } catch (e: Exception) {
            Log.e("SystemRepository", "Error getting used RAM: ${e.message}")
            return "N/A"
        }
    }
}