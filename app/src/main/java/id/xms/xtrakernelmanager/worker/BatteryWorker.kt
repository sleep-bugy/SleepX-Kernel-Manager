package id.xms.xtrakernelmanager.worker

import android.content.Context
import android.os.BatteryManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.xms.xtrakernelmanager.data.db.BatteryHistoryDao
import id.xms.xtrakernelmanager.data.db.BatteryHistoryEntry

@HiltWorker
class BatteryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batteryHistoryDao: BatteryHistoryDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val entry = BatteryHistoryEntry(
                timestamp = System.currentTimeMillis(),
                level = batteryLevel.toFloat()
            )

            batteryHistoryDao.insert(entry)

            // Also, clean up old entries (e.g., older than 3 days)
            val threeDaysAgo = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000
            batteryHistoryDao.deleteOldEntries(threeDaysAgo)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
