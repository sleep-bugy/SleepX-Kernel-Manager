package id.xms.xtrakernelmanager.data.model

data class RealtimeAggregatedInfo(
    val cpuInfo: RealtimeCpuInfo, // Ini akan menyertakan socModel
    val batteryInfo: BatteryInfo,
    val memoryInfo: MemoryInfo,
    val uptimeMillis: Long,
    val deepSleepMillis: Long
    // Anda bisa menambahkan properti lain di sini jika perlu
)