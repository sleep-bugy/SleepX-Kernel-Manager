package id.xms.xtrakernelmanager.data.model

import android.graphics.drawable.Drawable

// Model untuk statistik penggunaan baterai

data class BatteryStats(
    val screenOnTimeInSeconds: Long, // Total SOT in seconds for precision
    val topApps: List<AppUsage>, // daftar aplikasi dengan konsumsi daya tertinggi
    val batteryLevelHistory: List<Float> // History of battery level for the graph
)

data class AppUsage(
    val packageName: String, // Package name of the app
    val displayName: String, // User-friendly app name
    val icon: Drawable?,      // App icon
    val usagePercent: Float // persentase konsumsi daya
)
