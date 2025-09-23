package id.xms.xtrakernelmanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BatteryHistoryEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao
}
