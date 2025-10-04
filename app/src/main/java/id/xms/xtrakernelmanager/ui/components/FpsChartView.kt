package id.xms.xtrakernelmanager.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import java.util.*

class FpsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fpsHistory = LinkedList<Int>()
    private val maxDataPoints = 60 // Show last 60 FPS readings
    private val maxFps = 120 // Scale chart to 120 FPS max

    private val linePaint = Paint().apply {
        color = "#00FF00".toColorInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = "#40444444".toColorInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = "#AAFFFFFF".toColorInt()
        textSize = 8f
        isAntiAlias = true
    }

    // Preallocate path to avoid allocation during draw
    private val chartPath = Path()

    fun addFpsData(fps: Int) {
        fpsHistory.add(fps)
        if (fpsHistory.size > maxDataPoints) {
            fpsHistory.removeFirst()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * i / gridLines
            canvas.drawLine(0f, y, width, y, gridPaint)
        }

        // Draw FPS labels
        canvas.drawText("120", 2f, 10f, textPaint)
        canvas.drawText("60", 2f, height / 2 + 5f, textPaint)
        canvas.drawText("0", 2f, height - 2f, textPaint)

        // Draw FPS line chart
        if (fpsHistory.size > 1) {
            chartPath.reset()
            val stepX = width / (maxDataPoints - 1)

            fpsHistory.forEachIndexed { index, fps ->
                val x = index * stepX
                val y = height - (fps.toFloat() / maxFps * height)

                if (index == 0) {
                    chartPath.moveTo(x, y)
                } else {
                    chartPath.lineTo(x, y)
                }
            }

            canvas.drawPath(chartPath, linePaint)
        }
    }
}
