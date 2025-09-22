package id.xms.xtrakernelmanager.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.repository.GpuFpsRepository
import id.xms.xtrakernelmanager.util.RootUtils
import javax.inject.Inject

@AndroidEntryPoint
class FPSOverlayService : Service() {

    @Inject lateinit var gpuFpsRepo: GpuFpsRepository

    private lateinit var wm: WindowManager
    private var rootView: View? = null
    private var fpsText: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            val drmRaw = RootUtils.runCommandAsRoot("cat /sys/class/drm/sde-crtc-0/measured_fps") ?: "null"
            android.util.Log.d("DRM_CHAR", "drmRaw='$drmRaw'")

            val afterSpasi = drmRaw.substringAfter("fps:").substringBefore(" ").trim()
            val afterDur   = drmRaw.substringAfter("fps:").substringBefore("duration").trim()

            android.util.Log.d("DRM_CHAR", "afterSpasi='$afterSpasi'  hexa=${afterSpasi.toByteArray().contentToString()}")
            android.util.Log.d("DRM_CHAR", "afterDur  ='$afterDur'    hexa=${afterDur.toByteArray().contentToString()}")

            val floatSpasi = afterSpasi.toFloatOrNull()
            val floatDur   = afterDur.toFloatOrNull()
            android.util.Log.d("DRM_CHAR", "floatSpasi=$floatSpasi  floatDur=$floatDur")


            val fps = drmRaw
                .substringAfter("fps:")
                .substringBefore("duration")
                .trim()
                .toFloatOrNull()?.toInt() ?: 0
            android.util.Log.d("DRM_FINAL", "fps=$fps")

            // tampil
            fpsText?.text = "FPS: $fps"
            handler.postDelayed(this, 500)
        }
    }

    /* -------------- AOSP logcat fps -------------- */
    private fun getAospLogFps(): Int {
        val log = RootUtils.runCommandAsRoot("logcat -d -s SurfaceFlinger | tail -40") ?: return -1
        return Regex("""fps\s+(\d+\.?\d*)""").findAll(log)
            .mapNotNull { it.groupValues[1].toFloatOrNull() }
            .lastOrNull()?.toInt() ?: -1
    }

    private fun enableAospFpsLog() {
        RootUtils.runCommandAsRoot("setprop debug.sf.showfps 1")
        RootUtils.runCommandAsRoot("setprop ro.surface_flinger.debug_layer 1")
    }

    /* -------------- Choreographer fallback -------------- */
    private var choreoFps = 0
    private var frameCount = 0
    private var lastTime = 0L
    private val choreoCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val now = System.currentTimeMillis()
            if (lastTime == 0L) lastTime = now
            if (now - lastTime >= 1000) {
                choreoFps = frameCount
                frameCount = 0
                lastTime = now
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /* -------------- onCreate -------------- */
    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        rootView = LayoutInflater.from(this).inflate(R.layout.fps_overlay, null)
        fpsText = rootView?.findViewById(R.id.fpsTextView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
        wm.addView(rootView, params)

        enableAospFpsLog() // <-- hidupkan AOSP native fps log
        startForegroundService()
        Choreographer.getInstance().postFrameCallback(choreoCallback)
        handler.post(loop)
    }

    /* -------------- foreground -------------- */
    private fun startForegroundService() {
        val chanId = "fps_monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(chanId, "FPS Monitor", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
        val notification = NotificationCompat.Builder(this, chanId)
            .setContentTitle("FPS Overlay")
            .setContentText("Monitoring render fps")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .build()
        startForeground(1002, notification)
    }

    /* -------------- onDestroy -------------- */
    override fun onDestroy() {
        super.onDestroy()
        try { wm.removeView(rootView) } catch (ignore: Exception) {}
        handler.removeCallbacks(loop)
        Choreographer.getInstance().removeFrameCallback(choreoCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}