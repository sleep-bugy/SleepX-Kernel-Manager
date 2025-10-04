package id.xms.xtrakernelmanager.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.min

object OverlayDimensionUtils {

    private fun getScreenMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val bounds = windowMetrics.bounds
            DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                density = context.resources.displayMetrics.density
            }
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(metrics)
            metrics
        }
    }

    /**
     * Get responsive dimensions for FPS overlay based on screen size
     */
    fun getFpsOverlayDimensions(context: Context): OverlayDimensions {
        val metrics = getScreenMetrics(context)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // Use smaller dimension as base for calculations
        val baseDimension = min(screenWidth, screenHeight)

        // Calculate responsive sizes based on screen density and size
        val density = metrics.density

        return when {
            // Small screens (< 5 inches equivalent)
            baseDimension < (480 * density) -> OverlayDimensions(
                textSize = 10f,
                padding = (2 * density).toInt(),
                chartWidth = (80 * density).toInt(),
                chartHeight = (30 * density).toInt(),
                cornerRadius = (4 * density).toInt()
            )
            // Medium screens (5-6 inches equivalent)
            baseDimension < (640 * density) -> OverlayDimensions(
                textSize = 12f,
                padding = (4 * density).toInt(),
                chartWidth = (100 * density).toInt(),
                chartHeight = (35 * density).toInt(),
                cornerRadius = (6 * density).toInt()
            )
            // Large screens (> 6 inches)
            else -> OverlayDimensions(
                textSize = 14f,
                padding = (6 * density).toInt(),
                chartWidth = (120 * density).toInt(),
                chartHeight = (40 * density).toInt(),
                cornerRadius = (8 * density).toInt()
            )
        }
    }

    /**
     * Get responsive dimensions for game control overlay
     */
    fun getGameControlOverlayDimensions(context: Context): GameControlDimensions {
        val metrics = getScreenMetrics(context)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val density = metrics.density
        val baseDimension = min(screenWidth, screenHeight)

        // Calculate maximum width as percentage of screen width
        val maxWidthPercentage = when {
            screenWidth < (480 * density) -> 0.9f  // 90% for very small screens
            screenWidth < (640 * density) -> 0.8f  // 80% for small screens
            screenWidth < (800 * density) -> 0.7f  // 70% for medium screens
            else -> 0.6f  // 60% for large screens
        }

        val maxWidth = (screenWidth * maxWidthPercentage).toInt()

        return when {
            // Small screens
            baseDimension < (480 * density) -> GameControlDimensions(
                maxWidth = maxWidth,
                toggleButtonSize = (32 * density).toInt(),
                textSizeSmall = 10f,
                textSizeMedium = 12f,
                textSizeLarge = 14f,
                paddingSmall = (6 * density).toInt(),
                paddingMedium = (8 * density).toInt(),
                paddingLarge = (12 * density).toInt(),
                cornerRadius = (12 * density).toInt(),
                buttonHeight = (32 * density).toInt()
            )
            // Medium screens
            baseDimension < (640 * density) -> GameControlDimensions(
                maxWidth = maxWidth,
                toggleButtonSize = (36 * density).toInt(),
                textSizeSmall = 12f,
                textSizeMedium = 14f,
                textSizeLarge = 16f,
                paddingSmall = (8 * density).toInt(),
                paddingMedium = (12 * density).toInt(),
                paddingLarge = (16 * density).toInt(),
                cornerRadius = (16 * density).toInt(),
                buttonHeight = (36 * density).toInt()
            )
            // Large screens
            else -> GameControlDimensions(
                maxWidth = maxWidth,
                toggleButtonSize = (40 * density).toInt(),
                textSizeSmall = 14f,
                textSizeMedium = 16f,
                textSizeLarge = 18f,
                paddingSmall = (10 * density).toInt(),
                paddingMedium = (16 * density).toInt(),
                paddingLarge = (20 * density).toInt(),
                cornerRadius = (20 * density).toInt(),
                buttonHeight = (40 * density).toInt()
            )
        }
    }

    data class OverlayDimensions(
        val textSize: Float,
        val padding: Int,
        val chartWidth: Int,
        val chartHeight: Int,
        val cornerRadius: Int
    )

    data class GameControlDimensions(
        val maxWidth: Int,
        val toggleButtonSize: Int,
        val textSizeSmall: Float,
        val textSizeMedium: Float,
        val textSizeLarge: Float,
        val paddingSmall: Int,
        val paddingMedium: Int,
        val paddingLarge: Int,
        val cornerRadius: Int,
        val buttonHeight: Int
    )
}
