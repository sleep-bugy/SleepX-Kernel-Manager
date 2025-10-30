package id.xms.xtrakernelmanager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import id.xms.xtrakernelmanager.worker.BatteryWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltAndroidApp
class XtraApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var languageManager: id.xms.xtrakernelmanager.util.LanguageManager

    override fun onCreate() {
        super.onCreate()

        /* 1. Dark mode global */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        /* 2. Inject 430 dpi ke SELURUH activity / dialog */
        registerActivityLifecycleCallbacks(DensityInjector(430))

        /* 3. Terapkan bahasa tersimpan sedini mungkin */
        try {
            kotlinx.coroutines.runBlocking {
                val lang = languageManager.currentLanguage.first()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.code))
            }
        } catch (_: Exception) { /* ignore */ }

        /* 4. Schedule Battery Worker */
        setupRecurringWork()
        Log.d("XtraApp", "Battery worker scheduled & 430-dpi injector ready")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    private fun setupRecurringWork() {
        val workManager = WorkManager.getInstance(this)
        val batteryWorkRequest = PeriodicWorkRequestBuilder<BatteryWorker>(
            30, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "BatteryHistoryLogger",
            ExistingPeriodicWorkPolicy.KEEP,
            batteryWorkRequest
        )
    }

    /* ---------- Internal ---------- */
    private class DensityInjector(private val dpi: Int) : ActivityLifecycleCallbacks {
        override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) =
            inject(a)

        override fun onActivityStarted(a: Activity) {}
        override fun onActivityResumed(a: Activity) {}
        override fun onActivityPaused(a: Activity) {}
        override fun onActivityStopped(a: Activity) {}
        override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(a: Activity) {}

        private fun inject(ctx: Context) {
            val dm = ctx.resources.displayMetrics
            val cfg = ctx.resources.configuration
            dm.density = dpi / 160f
            dm.scaledDensity = dm.density * (cfg.fontScale.takeIf { it > 0 } ?: 1f)
            ctx.createConfigurationContext(cfg) // terapkan
        }
    }
}
