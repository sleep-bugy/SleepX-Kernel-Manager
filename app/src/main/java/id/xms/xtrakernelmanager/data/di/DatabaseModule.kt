package id.xms.xtrakernelmanager.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.xms.xtrakernelmanager.data.db.AppDatabase
import id.xms.xtrakernelmanager.data.db.BatteryHistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xtra_kernel_manager_db"
        ).build()
    }

    @Provides
    fun provideBatteryHistoryDao(appDatabase: AppDatabase): BatteryHistoryDao {
        return appDatabase.batteryHistoryDao()
    }
}
