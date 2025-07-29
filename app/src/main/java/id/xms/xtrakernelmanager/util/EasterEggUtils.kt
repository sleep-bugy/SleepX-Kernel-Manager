package id.xms.xtrakernelmanager.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import id.xms.xtrakernelmanager.data.repository.RootRepository
import javax.inject.Inject

class EasterEggUtils @Inject constructor(
    private val rootRepo: RootRepository
) {
    fun showIfForcedNonRoot(ctx: Context) {
        if (!rootRepo.isRooted()) {
            AlertDialog.Builder(ctx)
                .setTitle("System UI Destroyed")
                .setMessage("App will exit in 3 seconds")
                .setCancelable(false)
                .show()

            Handler(Looper.getMainLooper()).postDelayed({
                (ctx as Activity).finishAffinity()
            }, 3000)
        }
    }
}