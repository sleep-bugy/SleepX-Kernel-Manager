package id.xms.xtrakernelmanager.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.repository.GpuFpsRepository
import id.xms.xtrakernelmanager.ui.components.FpsChartView
import id.xms.xtrakernelmanager.util.RootUtils
import id.xms.xtrakernelmanager.util.OverlayDimensionUtils
import android.util.TypedValue
import javax.inject.Inject

@AndroidEntryPoint
class FPSOverlayService : Service() {

    @Inject lateinit var gpuFpsRepo: GpuFpsRepository

    private lateinit var wm: WindowManager
    private var rootView: View? = null
    private var fpsText: TextView? = null
    private var fpsChart: View? = null // Changed from FpsChartView to View

    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            val fps = getCurrentFps()

            // Update text display
            fpsText?.text = getString(R.string.fps_format, fps)

            // Update chart
            (fpsChart as? FpsChartView)?.addFpsData(fps)

            handler.postDelayed(this, 500)
        }
    }

    private fun getCurrentFps(): Int {
        // Try DRM method first
        val drmRaw = RootUtils.runCommandAsRoot("cat /sys/class/drm/sde-crtc-0/measured_fps") ?: ""
        if (drmRaw.contains("fps:")) {
            val fps = drmRaw
                .substringAfter("fps:")
                .substringBefore("duration")
                .trim()
                .toFloatOrNull()?.toInt() ?: 0
            if (fps > 0) return fps
        }

        // Fallback to AOSP method
        return getAospLogFps()
    }

    /* -------------- AOSP logcat fps -------------- */
    private fun getAospLogFps(): Int {
        val log = RootUtils.runCommandAsRoot("logcat -d -s SurfaceFlinger | tail -40") ?: return 60
        return Regex("""fps\s+(\d+\.?\d*)""").findAll(log)
            .mapNotNull { it.groupValues[1].toFloatOrNull() }
            .lastOrNull()?.toInt() ?: 60
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
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        // Get responsive dimensions for current screen
        val overlayDims = OverlayDimensionUtils.getFpsOverlayDimensions(this)

        // Create a temporary parent for proper layout inflation
        val tempParent = FrameLayout(this)
        rootView = LayoutInflater.from(this).inflate(R.layout.fps_overlay_with_chart, tempParent, false)

        fpsText = rootView?.findViewById(R.id.fpsTextView)
        fpsChart = rootView?.findViewById(R.id.fpsChartView)

        // Apply responsive dimensions to views
        applyResponsiveDimensions(overlayDims)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = overlayDims.padding
            y = overlayDims.padding * 3
        }
        wm.addView(rootView, params)

        enableAospFpsLog()
        startForegroundService()
        Choreographer.getInstance().postFrameCallback(choreoCallback)
        handler.post(loop)
    }

    private fun applyResponsiveDimensions(dims: OverlayDimensionUtils.OverlayDimensions) {
        try {
            // Apply responsive text size
            fpsText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, dims.textSize)

            // Apply responsive padding to root container
            val rootContainer = rootView?.findViewById<View>(R.id.overlayContainer)
            rootContainer?.setPadding(dims.padding, dims.padding, dims.padding, dims.padding)

            // Apply responsive dimensions to chart
            fpsChart?.layoutParams = fpsChart?.layoutParams?.apply {
                width = dims.chartWidth
                height = dims.chartHeight
            }
        } catch (e: Exception) {
            android.util.Log.e("FPSOverlay", "Error applying responsive dimensions: ${e.message}")
        }
    }

    /* -------------- foreground -------------- */
    private fun startForegroundService() {
        val chanId = "fps_monitor_channel"
        val chan = NotificationChannel(chanId, "FPS Monitor", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(chan)

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