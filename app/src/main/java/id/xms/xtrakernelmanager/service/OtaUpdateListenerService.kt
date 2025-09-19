package id.xms.xtrakernelmanager.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.model.UpdateInfo

class OtaUpdateListenerService : Service() {
    private lateinit var dbRef: DatabaseReference
    private var lastNotifiedVersion: String? = null
    private val channelId = "ota_update_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Start foreground immediately with a minimal notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("OTA Update Listener Running")
            .setContentText("Listening for OTA updates...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(notificationId, notification)

        dbRef = FirebaseDatabase.getInstance().getReference("update")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val update = snapshot.getValue(UpdateInfo::class.java)
                val localVersion = try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                } catch (e: Exception) { "" }

                if (update != null && update.version.isNotBlank() && isNewerVersion(update.version, localVersion)) {
                    if (lastNotifiedVersion != update.version) {
                        showUpdateNotification(update)
                        lastNotifiedVersion = update.version
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".", "-").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
            val r = remoteParts.getOrNull(i) ?: 0
            val l = localParts.getOrNull(i) ?: 0
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun showUpdateNotification(update: UpdateInfo) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("OTA Update Available")
            .setContentText("Version ${update.version} is available")
            .setSmallIcon(R.drawable.ic_update)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Version ${update.version} is available.\n${update.changelog}"))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notification = builder.build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId + 1, notification)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "OTA Updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
