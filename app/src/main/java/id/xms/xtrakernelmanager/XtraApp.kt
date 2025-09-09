package id.xms.xtrakernelmanager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import android.util.Log

@HiltAndroidApp
class XtraApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        /* 1. Dark mode global */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        /* 2. Inject 430 dpi ke SELURUH activity / dialog */
        registerActivityLifecycleCallbacks(DensityInjector(430))

        /* 3. WorkManager */
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        )
        Log.d("XtraApp", "WorkManager & 430-dpi injector ready")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

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