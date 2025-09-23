package id.xms.xtrakernelmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BatteryHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BatteryHistoryEntry)

    @Query("SELECT * FROM battery_history WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getHistorySince(since: Long): List<BatteryHistoryEntry>

    @Query("DELETE FROM battery_history WHERE timestamp < :before")
    suspend fun deleteOldEntries(before: Long)

}
