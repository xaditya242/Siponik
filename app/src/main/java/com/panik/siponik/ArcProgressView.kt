package com.panik.siponik

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.math.*

class ArcProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var progress = 0f            // Suhu sekarang
    var max = 50f                // Suhu maksimum

    var baseArcColor = ContextCompat.getColor(context, R.color.hijau_muda)
    var baseArcWidth = 28f

    var progressArcColor = ContextCompat.getColor(context, R.color.hijau_tua)
    var progressArcWidth = 15f

    var capCircleRadius = 25f
    var capCircleColor = ContextCompat.getColor(context, R.color.hijau_tua)

    var textColor = ContextCompat.getColor(context, R.color.hijau_muda)
    var textSize = 40f
    var textFont: Typeface? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val maxStrokeWidth = max(baseArcWidth, progressArcWidth)
        val radius = (min(width, height * 2f) - maxStrokeWidth) / 2f

        val centerX = width / 2
        val centerY = height

        val rect = RectF(
            centerX - radius,
            centerY - radius * 2,
            centerX + radius,
            centerY
        )

        // Arc dasar
        basePaint.color = baseArcColor
        basePaint.strokeWidth = baseArcWidth
        canvas.drawArc(rect, 180f, 180f, false, basePaint)

        // Arc progress
        val sweepAngle = (progress / max).coerceIn(0f, 1f) * 180f
        progressPaint.color = progressArcColor
        progressPaint.strokeWidth = progressArcWidth
        canvas.drawArc(rect, 180f, sweepAngle, false, progressPaint)

        // Titik ujung arc
        val angleInRadians = Math.toRadians((180 + sweepAngle).toDouble())
        val capX = (centerX + radius * cos(angleInRadians)).toFloat()
        val capY = (centerY + radius * sin(angleInRadians)).toFloat()

        capPaint.color = capCircleColor
        canvas.drawCircle(capX, capY, capCircleRadius, capPaint)

        // Teks suhu
        textPaint.color = textColor
        textPaint.textSize = textSize
        textFont?.let { textPaint.typeface = it }

        val displayText = "${progress.toInt()}°C"
        val textY = centerY - radius / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(displayText, centerX, textY, textPaint)
    }

    fun setProgressValue(value: Float) {
        progress = value.coerceIn(0f, max)
        invalidate()
    }

    fun setMaxValue(maxVal: Float) {
        max = maxVal
        invalidate()
    }
}
