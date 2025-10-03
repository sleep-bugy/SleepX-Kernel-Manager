package id.xms.xtrakernelmanager.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.model.AppGameSettings
import id.xms.xtrakernelmanager.data.repository.GameControlRepository
import id.xms.xtrakernelmanager.data.repository.GpuFpsRepository
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.*
import java.util.LinkedList
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class GameControlService : Service() {

    @Inject lateinit var gpuFpsRepo: GpuFpsRepository
    @Inject lateinit var gameControlRepo: GameControlRepository
    @Inject lateinit var preferenceManager: id.xms.xtrakernelmanager.utils.PreferenceManager

    private lateinit var wm: WindowManager
    private var rootView: View? = null
    private var overlayRootContainer: ConstraintLayout? = null
    private var currentFpsText: TextView? = null
    private var cpuLoadText: TextView? = null
    private var gpuLoadText: TextView? = null
    private var cpuGovernorText: TextView? = null
    private var gpuGovernorText: TextView? = null
    private var gameControlPanel: View? = null
    private var gameControlToggle: View? = null  // Use generic View to support different widget types

    // Performance mode buttons (now chips)
    private var defaultModeButton: Chip? = null
    private var batteryModeButton: Chip? = null
    private var performanceModeButton: Chip? = null

    // Quick action buttons
    private var dndButton: MaterialButton? = null
    private var clearAppsButton: MaterialButton? = null
    private var settingsButton: MaterialButton? = null
    private var collapseButton: MaterialButton? = null

    // Window manager parameters for positioning the overlay
    private var layoutParams: WindowManager.LayoutParams? = null

    // Current performance mode
    private var currentMode = "default"

    // Current app package
    private var currentAppPackage = ""
    private var currentAppSettings: AppGameSettings? = null

    // For tracking drag motion
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Main update loop
    private val updateTask = object : Runnable {
        override fun run() {
            // Run heavy operations in background thread to avoid UI lag
            serviceScope.launch(Dispatchers.IO) {
                updateFps()
                updateSystemStats()

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Check master toggle first before forcing visibility
                    val masterEnabled = preferenceManager.getFpsMonitorEnabled()

                    // Force check visibility on each update to ensure overlay behaves correctly
                    if (!masterEnabled && rootView?.visibility == View.VISIBLE) {
                        android.util.Log.d("GameControl_Visibility", "Master toggle disabled - hiding overlay")
                        rootView?.visibility = View.GONE
                    } else if (masterEnabled && rootView?.visibility != View.VISIBLE && shouldShowOverlay()) {
                        android.util.Log.d("GameControl_Visibility", "Master toggle enabled - showing overlay")
                        rootView?.visibility = View.VISIBLE
                    }
                }
            }

            // Increase update interval to reduce system load
            handler.postDelayed(this, 1000) // Changed from 500ms to 1000ms
        }
    }

    private fun shouldShowOverlay(): Boolean {
        // Check master toggle first - if disabled, don't show overlay
        val masterEnabled = preferenceManager.getFpsMonitorEnabled()
        if (!masterEnabled) {
            return false
        }

        // If master is enabled, show overlay based on app settings or at least FPS counter
        return currentAppSettings?.enabled == true ||
               currentAppSettings?.showFpsCounter == true ||
               masterEnabled // Show basic FPS counter when master is enabled but no app-specific settings
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlay()
        startForegroundService()
        handler.post(updateTask)

        // Observe current foreground app
        serviceScope.launch {
            currentAppPackage = getCurrentForegroundApp()
            currentAppSettings = gameControlRepo.getAppSettings(currentAppPackage)

            // Check initial app state and show overlay accordingly
            // Note: We no longer check global game control setting - only per-app settings
            withContext(Dispatchers.Main) {
                updateOverlayVisibility()
            }

            // Check foreground app periodically
            while (true) {
                val newForegroundApp = getCurrentForegroundApp()
                if (newForegroundApp != currentAppPackage) {
                    currentAppPackage = newForegroundApp
                    currentAppSettings = gameControlRepo.getAppSettings(currentAppPackage)

                    // Update overlay based purely on per-app settings
                    withContext(Dispatchers.Main) {
                        updateOverlayVisibility()
                    }
                }
                delay(1000) // Check every second
            }
        }
    }

    private fun updateOverlayVisibility() {
        // Check master toggle first - if disabled, hide overlay completely
        val masterEnabled = preferenceManager.getFpsMonitorEnabled()
        if (!masterEnabled) {
            rootView?.visibility = View.GONE
            android.util.Log.d("GameControl_Overlay", "Master toggle disabled - hiding overlay")
            return
        }

        // Master toggle is enabled - show overlay based on app settings
        rootView?.visibility = View.VISIBLE

        when {
            currentAppSettings?.enabled == true -> {
                // Full game control enabled for this app - show complete overlay with toggle button
                gameControlToggle?.visibility = View.VISIBLE
                gameControlPanel?.visibility = View.GONE // Start collapsed
                applyAppSettings(currentAppSettings!!)
                android.util.Log.d("GameControl_Overlay", "Showing full overlay for ${currentAppPackage}")
            }
            currentAppSettings?.showFpsCounter == true -> {
                // Only FPS counter enabled for this app - still show toggle button for tools access
                gameControlToggle?.visibility = View.VISIBLE
                gameControlPanel?.visibility = View.GONE
                android.util.Log.d("GameControl_Overlay", "Showing FPS counter with toggle for ${currentAppPackage}")
            }
            else -> {
                // Default: show basic overlay with toggle button when master is enabled
                gameControlToggle?.visibility = View.VISIBLE
                gameControlPanel?.visibility = View.GONE
                android.util.Log.d("GameControl_Overlay", "Showing basic overlay with toggle for ${currentAppPackage}")
            }
        }
    }

    private fun getCurrentForegroundApp(): String {
        // Try non-root method first using dumpsys activity
        try {
            val process = Runtime.getRuntime().exec("dumpsys activity activities")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            // Parse package name from dumpsys output
            val regex = """mResumedActivity.*\s+([a-zA-Z0-9_.]+)/.*""".toRegex()
            val matchResult = regex.find(output)
            val packageName = matchResult?.groupValues?.getOrNull(1) ?: ""
            if (packageName.isNotEmpty()) {
                return packageName
            }
        } catch (e: Exception) {
            android.util.Log.d("GameControl_ForegroundApp", "Non-root method failed: ${e.message}")
        }

        // Fallback to root method if available
        val hasRoot = RootUtils.isDeviceRooted()
        if (hasRoot) {
            val cmd = "dumpsys activity activities | grep mResumedActivity"
            val result = RootUtils.runCommandAsRoot(cmd) ?: ""

            // Parse package name from dumpsys output
            val regex = """mResumedActivity.*\s+([a-zA-Z0-9_.]+)/.*""".toRegex()
            val matchResult = regex.find(result)
            return matchResult?.groupValues?.getOrNull(1) ?: ""
        }

        return ""
    }

    private fun applyAppSettings(settings: AppGameSettings) {
        // Apply performance mode
        if (settings.defaultPerformanceMode.isNotEmpty()) {
            setPerformanceMode(settings.defaultPerformanceMode)
        }

        // Apply DND if enabled
        if (settings.dndEnabled) {
            setDoNotDisturb(true)
        }

        if (settings.clearBackgroundOnLaunch) {
            clearBackgroundApps()
        }
    }

    private fun setupOverlay() {
        var themedContext: Context = android.view.ContextThemeWrapper(this, R.style.Theme_GameOverlay)
        themedContext = DynamicColors.wrapContextIfAvailable(themedContext)

        val baseInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val themedInflater = baseInflater.cloneInContext(themedContext)

        try {
            rootView = themedInflater.inflate(R.layout.game_control_overlay_m3, null)
        } catch (e: Throwable) {
            android.util.Log.e("GameControlService", "Failed to inflate M3 overlay: ${e.message}. Falling back to legacy overlay.")
            rootView = LayoutInflater.from(this).inflate(R.layout.game_control_overlay, null)
        }
        overlayRootContainer = rootView?.findViewById(R.id.overlayRootContainer)
        currentFpsText = rootView?.findViewById(R.id.currentFpsText)
        cpuLoadText = rootView?.findViewById(R.id.cpuLoadText)
        gpuLoadText = rootView?.findViewById(R.id.gpuLoadText)
        cpuGovernorText = rootView?.findViewById(R.id.cpuGovernorText)
        gpuGovernorText = rootView?.findViewById(R.id.gpuGovernorText)
        gameControlPanel = rootView?.findViewById(R.id.gameControlPanel)
        gameControlToggle = rootView?.findViewById(R.id.gameControlToggle)

        // buttons
        defaultModeButton = rootView?.findViewById(R.id.defaultModeButton)
        batteryModeButton = rootView?.findViewById(R.id.batteryModeButton)
        performanceModeButton = rootView?.findViewById(R.id.performanceModeButton)
        dndButton = rootView?.findViewById(R.id.dndButton)
        clearAppsButton = rootView?.findViewById(R.id.clearAppsButton)
        settingsButton = rootView?.findViewById(R.id.settingsButton)
        collapseButton = rootView?.findViewById(R.id.collapseButton)

        // Setup click listeners
        val toggleButtonCard = rootView?.findViewById<View>(R.id.toggleButtonCard)
        val togglePanel: () -> Unit = {
            if (gameControlPanel?.visibility == View.VISIBLE) {
                gameControlPanel?.visibility = View.GONE
            } else {
                gameControlPanel?.visibility = View.VISIBLE
            }
        }
        toggleButtonCard?.setOnClickListener { togglePanel() }
        gameControlToggle?.setOnClickListener { togglePanel() }

        collapseButton?.setOnClickListener {
            gameControlPanel?.visibility = View.GONE
        }

        defaultModeButton?.setOnClickListener {
            setPerformanceMode("default")
        }

        batteryModeButton?.setOnClickListener {
            setPerformanceMode("battery")
        }

        performanceModeButton?.setOnClickListener {
            setPerformanceMode("performance")
        }

        dndButton?.setOnClickListener {
            toggleDoNotDisturb()
        }

        clearAppsButton?.setOnClickListener {
            clearBackgroundApps()
        }

        settingsButton?.setOnClickListener {
            // Open game settings for current app
            val intent = Intent(this, Class.forName("id.xms.xtrakernelmanager.ui.GameControlSettingsActivity"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("package_name", currentAppPackage)
            startActivity(intent)
        }

        // Setup drag listener for the entire overlay
        setupDragToMove()

        // Add view to window manager
        layoutParams = WindowManager.LayoutParams(
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

        wm.addView(rootView, layoutParams)

        // Hide panel initially, only show toggle button
        gameControlPanel?.visibility = View.GONE
    }

    private fun setupDragToMove() {
        val toggleButtonCard = rootView?.findViewById<View>(R.id.toggleButtonCard)

        toggleButtonCard?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        layoutParams?.x = (initialX + dx).toInt()
                        layoutParams?.y = (initialY + dy).toInt()

                        // Apply the updated position
                        if (rootView != null && layoutParams != null) {
                            wm.updateViewLayout(rootView, layoutParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Check if this is a click or a drag
                        val dx = abs(event.rawX - initialTouchX)
                        val dy = abs(event.rawY - initialTouchY)

                        // If moved less than 10px, consider it a click
                        if (dx < 10 && dy < 10) {
                            toggleButtonCard?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Also allow dragging from the panel title area
        val panelTitle = rootView?.findViewById<View>(R.id.panelTitle)
        panelTitle?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Calculate how far the user's finger has moved
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY

                        // Move the view
                        layoutParams?.x = (initialX + dx).toInt()
                        layoutParams?.y = (initialY + dy).toInt()

                        // Apply the updated position
                        if (rootView != null && layoutParams != null) {
                            wm.updateViewLayout(rootView, layoutParams)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun updateFps() {
        val fps = getFps()
        handler.post {
            currentFpsText?.text = if (fps > 0) "$fps FPS" else "-- FPS"
        }
    }

    private fun getFps(): Int {
        // For non-root devices, try alternative methods first
        android.util.Log.d("GameControl_FPS", "Attempting to get FPS data")
        try {
            val process = Runtime.getRuntime().exec("dumpsys SurfaceFlinger --latency")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            // Log the output for debugging
            android.util.Log.d("GameControl_Dumpsys", "SurfaceFlinger output length: ${output.length}")

            // Parse latency data to calculate FPS
            val lines = output.split("\n").filter { it.trim().isNotEmpty() }
            if (lines.size > 3) {
                val timestamps = lines.drop(1).take(10).mapNotNull { line ->
                    val parts = line.split("\t")
                    if (parts.size >= 2) parts[1].toLongOrNull() else null
                }.filter { it > 0 }

                if (timestamps.size >= 2) {
                    val avgInterval = (timestamps.last() - timestamps.first()) / (timestamps.size - 1)
                    val fps = if (avgInterval > 0) (1_000_000_000.0 / avgInterval).toInt() else 0
                    if (fps in 1..300) {
                        android.util.Log.d("GameControl_FPS", "Non-root FPS calculated: $fps")
                        return fps // Reasonable FPS range
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("GameControl_Dumpsys", "Dumpsys failed: ${e.message}")
        }

        // Try root methods only if available
        val hasRoot = RootUtils.isDeviceRooted()
        if (hasRoot) {
            // Try to get from DRM first
            val drmRaw = RootUtils.runCommandAsRoot("cat /sys/class/drm/sde-crtc-0/measured_fps") ?: ""
            android.util.Log.d("GameControl_DRM", "drmRaw='$drmRaw'")

            if (drmRaw.contains("fps:")) {
                val fps = drmRaw
                    .substringAfter("fps:")
                    .substringBefore("duration")
                    .trim()
                    .toFloatOrNull()?.toInt() ?: 0

                android.util.Log.d("GameControl_DRM", "DRM FPS parsed: $fps")
                if (fps > 0) return fps
            }

            // Fallback to AOSP FPS log
            val log = RootUtils.runCommandAsRoot("logcat -d -s SurfaceFlinger | tail -40") ?: ""
            val aospFps = Regex("""fps\s+(\d+\.?\d*)""").findAll(log)
                .mapNotNull { it.groupValues[1].toFloatOrNull() }
                .lastOrNull()?.toInt() ?: 0

            android.util.Log.d("GameControl_AOSP", "AOSP FPS parsed: $aospFps")
            if (aospFps > 0) return aospFps

            // Enable AOSP FPS logging if not enabled
            RootUtils.runCommandAsRoot("setprop debug.sf.showfps 1")
            RootUtils.runCommandAsRoot("setprop ro.surface_flinger.debug_layer 1")
        } else {
            // Non-root fallback - estimate FPS based on time between frame updates
            try {
                val estimatedFps = estimateFrameRate()
                if (estimatedFps > 0) {
                    android.util.Log.d("GameControl_FPS", "Estimated FPS: $estimatedFps")
                    return estimatedFps
                }
            } catch (e: Exception) {
                android.util.Log.d("GameControl_FPS", "FPS estimation failed: ${e.message}")
            }
        }

        // If all else fails, return 0 to indicate no FPS data available
        android.util.Log.d("GameControl_FPS", "No FPS data available, returning 0")
        return 0
    }

    // Frame time tracking for non-root FPS estimation
    private val frameTimes = LinkedList<Long>()
    private var lastFrameTime = 0L

    private fun estimateFrameRate(): Int {
        val now = System.nanoTime()

        // First call or after long pause
        if (lastFrameTime == 0L || now - lastFrameTime > 1_000_000_000) {
            lastFrameTime = now
            frameTimes.clear()
            return 0
        }

        // Add time difference to the queue
        val deltaTime = now - lastFrameTime
        lastFrameTime = now

        frameTimes.add(deltaTime)

        // Keep only last 10 frame times
        while (frameTimes.size > 10) {
            frameTimes.removeFirst()
        }

        // Need at least a few samples for reasonable estimation
        if (frameTimes.size < 3) {
            return 0
        }

        // Calculate average frame time and convert to FPS
        val avgFrameTime = frameTimes.average()
        return if (avgFrameTime > 0) (1_000_000_000 / avgFrameTime).toInt() else 0
    }

    private fun updateSystemStats() {
        // For non-root devices, show placeholder data or skip certain stats
        val hasRoot = RootUtils.isDeviceRooted()

        if (hasRoot) {
            // Use batch commands for better performance on low-end devices
            val commands = listOf(
                "cat /proc/stat | grep 'cpu '",
                "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                "cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null || echo '0'",
                "cat /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || cat /sys/devices/platform/mali.0/power_policy 2>/dev/null || echo 'unknown'"
            )

            val results = RootUtils.runCommandsAsRoot(commands)

            // Update UI on main thread
            handler.post {
                if (results.size >= 4) {
                    // Parse CPU load
                    val cpuStat = results[0]
                    val cpuLoad = parseCpuLoad(cpuStat)
                    cpuLoadText?.text = "$cpuLoad%"

                    // CPU Governor
                    val cpuGovernor = results[1].trim()
                    cpuGovernorText?.text = cpuGovernor

                    // GPU Load
                    val gpuLoad = results[2].trim().toIntOrNull() ?: 0
                    gpuLoadText?.text = "$gpuLoad%"

                    // GPU Governor
                    val gpuGovernor = results[3].trim()
                    gpuGovernorText?.text = gpuGovernor
                } else {
                    // Fallback to individual commands if batch failed
                    updateSystemStatsIndividually()
                }
            }
        } else {
            // Non-root fallback - update UI on main thread
            handler.post {
                cpuLoadText?.text = "N/A"
                cpuGovernorText?.text = "Non-root"
                gpuLoadText?.text = "N/A"
                gpuGovernorText?.text = "Non-root"
            }
        }
    }

    private fun updateSystemStatsIndividually() {
        // Fallback method using individual commands
        val cpuLoad = getCpuLoad()
        val cpuGovernor = getCpuGovernor()
        cpuLoadText?.text = "$cpuLoad%"
        cpuGovernorText?.text = cpuGovernor

        val gpuLoad = getGpuLoad()
        val gpuGovernor = getGpuGovernor()
        gpuLoadText?.text = "$gpuLoad%"
        gpuGovernorText?.text = gpuGovernor
    }

    private fun parseCpuLoad(cpuStat: String): Int {
        return try {
            val parts = cpuStat.split(" ").filter { it.isNotEmpty() }
            if (parts.size < 8) return 0

            val total = parts.subList(1, parts.size).sumOf { it.toLongOrNull() ?: 0 }
            val idle = parts[4].toLongOrNull() ?: 0

            val cpuUsage = 100 * (1 - idle.toFloat() / total)
            cpuUsage.toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            0
        }
    }

    private fun getCpuLoad(): Int {
        val result = RootUtils.runCommandAsRoot("cat /proc/stat | grep 'cpu '") ?: return 0

        // Parse CPU utilization
        val parts = result.split(" ").filter { it.isNotEmpty() }
        if (parts.size < 8) return 0

        val total = parts.subList(1, parts.size).sumOf { it.toLongOrNull() ?: 0 }
        val idle = parts[4].toLongOrNull() ?: 0

        val cpuUsage = 100 * (1 - idle.toFloat() / total)
        return cpuUsage.toInt().coerceIn(0, 100)
    }

    private fun getCpuGovernor(): String {
        val result = RootUtils.runCommandAsRoot("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor") ?: ""
        return result.trim()
    }

    private fun getGpuLoad(): Int {
        val adrenoResult = RootUtils.runCommandAsRoot("cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage") ?: ""
        if (adrenoResult.trim().matches(Regex("\\d+"))) {
            return adrenoResult.trim().toIntOrNull() ?: 0
        }
        val maliResult = RootUtils.runCommandAsRoot("cat /sys/devices/platform/mali.0/utilization") ?: ""
        if (maliResult.contains("=")) {
            return maliResult.substringAfter("=").trim().toIntOrNull() ?: 0
        }

        return 0
    }

    private fun getGpuGovernor(): String {
        val adrenoResult = RootUtils.runCommandAsRoot("cat /sys/class/kgsl/kgsl-3d0/devfreq/governor") ?: ""
        if (adrenoResult.isNotEmpty()) {
            return adrenoResult.trim()
        }
        val maliResult = RootUtils.runCommandAsRoot("cat /sys/devices/platform/mali.0/power_policy") ?: ""
        if (maliResult.isNotEmpty()) {
            return maliResult.trim()
        }

        return "unknown"
    }

    private fun setPerformanceMode(mode: String) {
        currentMode = mode

        // Update UI
        defaultModeButton?.alpha = if (mode == "default") 1.0f else 0.5f
        batteryModeButton?.alpha = if (mode == "battery") 1.0f else 0.5f
        performanceModeButton?.alpha = if (mode == "performance") 1.0f else 0.5f

        // Use batch commands for better performance on low-end devices
        val commands = when (mode) {
            "default" -> listOf(
                "echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                "echo schedutil > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true"
            )
            "battery" -> listOf(
                "echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                "echo powersave > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true"
            )
            "performance" -> listOf(
                "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                "echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true"
            )
            else -> emptyList()
        }

        if (commands.isNotEmpty()) {
            RootUtils.runCommandsAsRoot(commands)
        }
        currentAppPackage?.let { pkg ->
            if (pkg.isNotEmpty()) {
                serviceScope.launch {
                    val settings = gameControlRepo.getAppSettings(pkg) ?: AppGameSettings(pkg)
                    settings.defaultPerformanceMode = mode
                    gameControlRepo.saveAppSettings(settings)
                }
            }
        }
    }

    private fun toggleDoNotDisturb() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Toggle DND mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentFilter = notificationManager.currentInterruptionFilter
            val newFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                NotificationManager.INTERRUPTION_FILTER_NONE
            }

            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(newFilter)

                // Visual feedback
                dndButton?.alpha = if (newFilter == NotificationManager.INTERRUPTION_FILTER_NONE) 1.0f else 0.5f
                currentAppPackage?.let { pkg ->
                    if (pkg.isNotEmpty()) {
                        serviceScope.launch {
                            val settings = gameControlRepo.getAppSettings(pkg) ?: AppGameSettings(pkg)
                            settings.dndEnabled = newFilter == NotificationManager.INTERRUPTION_FILTER_NONE
                            gameControlRepo.saveAppSettings(settings)
                        }
                    }
                }
            } else {
                val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private fun setDoNotDisturb(enable: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            val newFilter = if (enable) {
                NotificationManager.INTERRUPTION_FILTER_NONE
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            notificationManager.setInterruptionFilter(newFilter)

            // Visual feedback
            dndButton?.alpha = if (enable) 1.0f else 0.5f
        }
    }

    private fun clearBackgroundApps() {
        // Force stop all background apps except system apps and current app
        val cmd = """
            for pkgItem in $(pm list packages -3); do
                pkgName=$(echo ${'$'}pkgItem | cut -d: -f2)
                if [ "${'$'}pkgName" != "$currentAppPackage" ]; then
                    am force-stop ${'$'}pkgName
                fi
            done
        """.trimIndent()

        RootUtils.runCommandAsRoot(cmd)

        // Visual feedback (briefly highlight the button)
        clearAppsButton?.alpha = 1.0f
        handler.postDelayed({
            clearAppsButton?.alpha = 0.5f
        }, 500)
    }

    private fun startForegroundService() {
        val chanId = "game_control_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(chanId, "Game Control", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
        val notification = NotificationCompat.Builder(this, chanId)
            .setContentTitle("Game Control")
            .setContentText("Game performance monitoring active")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .build()
        startForeground(1002, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { wm.removeView(rootView) } catch (ignore: Exception) {}
        handler.removeCallbacks(updateTask)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
