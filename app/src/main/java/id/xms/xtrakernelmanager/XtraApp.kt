package id.xms.xtrakernelmanager

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import android.util.Log

@HiltAndroidApp
class XtraApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Inisialisasi WorkManager secara manual
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG) // Logging untuk debug
                .build()
        )
        Log.d("XtraApp", "WorkManager initialized successfully")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG) // Logging untuk debugging
            .build()
}