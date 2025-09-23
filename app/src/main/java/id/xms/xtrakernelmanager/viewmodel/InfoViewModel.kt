package id.xms.xtrakernelmanager.viewmodel

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.db.BatteryHistoryDao
import id.xms.xtrakernelmanager.data.model.AppUsage
import id.xms.xtrakernelmanager.data.model.BatteryStats
import id.xms.xtrakernelmanager.data.model.SystemInfo
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SystemRepository,
    private val batteryHistoryDao: BatteryHistoryDao
) : ViewModel() {

    // SharedPreferences untuk caching SoT
    private val prefs = context.getSharedPreferences("sot_prefs", Context.MODE_PRIVATE)

    /* StateFlow yang SELALU diisi dari SharedPreferences saat init */
    private val _sotCached = MutableStateFlow(prefs.getLong("sot_cached", 0L))
    val sotCached: StateFlow<Long> = _sotCached.asStateFlow()

    /* Dipanggil UI saat tekan Refresh */


    val system: StateFlow<SystemInfo?> = flow { emit(repo.getSystemInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)


    val batteryStats: StateFlow<BatteryStats?> = flow {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            val topApps = getTopAppUsages()
            val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val historyFromDb = batteryHistoryDao.getHistorySince(since)
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
            val raw = (historyFromDb.map { it.level } + current)

            val finalHistory = raw.ifEmpty { listOf(current, current) }   // minimal 2 titik
            Log.d("BATT", "History size = ${finalHistory.size}, content = $finalHistory")

            emit(BatteryStats(screenOnTimeInSeconds = 0L, topApps = topApps, batteryLevelHistory = finalHistory))
            delay(5_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /* Manual refresh SoT – dipanggil UI */
    suspend fun refreshScreenOnTime(): Long =
        repo.getScreenOnTimeSeconds().also { new ->
            prefs.edit().putLong("sot_cached", new).apply()
            _sotCached.value = new        // update Flow supaya UI langsung ikut
        }

    /* Mendapatkan daftar aplikasi dengan konsumsi daya tertinggi dalam 24 jam terakhir */
    private suspend fun getTopAppUsages(): List<AppUsage> = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(1)
        val list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        if (list.isNullOrEmpty()) return@withContext emptyList()

        val total = list.sumOf { it.totalTimeInForeground }.takeIf { it > 0 } ?: return@withContext emptyList()
        val pm = context.packageManager

        list.asSequence()
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(5)
            .mapNotNull { stat ->
                runCatching {
                    val ai = pm.getApplicationInfo(stat.packageName, 0)
                    AppUsage(
                        packageName = stat.packageName,
                        displayName = pm.getApplicationLabel(ai).toString(),
                        icon = pm.getApplicationIcon(ai),
                        usagePercent = (stat.totalTimeInForeground.toFloat() / total) * 100
                    )
                }.getOrNull()
            }
            .toList()
    }
}