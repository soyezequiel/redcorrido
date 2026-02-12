package com.routerecorder.service.videorender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.routerecorder.core.common.util.FormatUtils
import com.routerecorder.core.domain.model.RoutePhoto
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.model.VideoConfig

/**
 * Renders individual video frames onto a Canvas.
 *
 * Responsible for:
 * - Drawing the progressive route polyline
 * - Drawing the current position indicator
 * - Drawing the metrics overlay panel
 * - Compositing photos with fade transitions
 */
class FrameRenderer(
    private val config: VideoConfig,
    private val allPoints: List<TrackPoint>,
    private val photos: List<RoutePhoto>,
    private val startTimeMs: Long,
    private val totalDurationMs: Long
) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.lineColor
        strokeWidth = config.lineWidthPx
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.lineColor
        style = Paint.Style.FILL
        alpha = 100
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        textSize = 28f
        typeface = Typeface.DEFAULT
    }

    private val overlayPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val bgPaint = Paint().apply {
        color = config.backgroundColor
        style = Paint.Style.FILL
    }

    // Precompute coordinate bounds for mapping GPS → screen
    private val minLat: Double
    private val maxLat: Double
    private val minLng: Double
    private val maxLng: Double
    private val mapPadding = 100f
    private val mapWidth: Float
    private val mapHeight: Float

    init {
        var mnLat = Double.MAX_VALUE
        var mxLat = Double.MIN_VALUE
        var mnLng = Double.MAX_VALUE
        var mxLng = Double.MIN_VALUE
        for (p in allPoints) {
            if (p.latitude < mnLat) mnLat = p.latitude
            if (p.latitude > mxLat) mxLat = p.latitude
            if (p.longitude < mnLng) mnLng = p.longitude
            if (p.longitude > mxLng) mxLng = p.longitude
        }
        // Add small margin to prevent points at exact edge
        val latMargin = (mxLat - mnLat) * 0.1
        val lngMargin = (mxLng - mnLng) * 0.1
        minLat = mnLat - latMargin
        maxLat = mxLat + latMargin
        minLng = mnLng - lngMargin
        maxLng = mxLng + lngMargin

        // Map occupies top 65% of screen, leaving room for metrics panel
        mapWidth = config.width - 2 * mapPadding
        mapHeight = config.height * 0.65f - 2 * mapPadding
    }

    /**
     * Render a single frame for the given progress (0.0 to 1.0).
     * @param canvas the Canvas to draw on (already sized to config dimensions)
     * @param progress animation progress from 0.0 (start) to 1.0 (end)
     * @param photoBitmap optional photo bitmap to composite (already decoded)
     * @param photoAlpha alpha for photo transition (0.0 to 1.0)
     */
    fun renderFrame(
        canvas: Canvas,
        progress: Float,
        photoBitmap: Bitmap? = null,
        photoAlpha: Float = 0f
    ) {
        // 1. Background
        canvas.drawRect(0f, 0f, config.width.toFloat(), config.height.toFloat(), bgPaint)

        // 2. Route polyline (progressive)
        val pointsUpTo = ((allPoints.size - 1) * progress).toInt().coerceIn(0, allPoints.size - 1)
        drawRoute(canvas, pointsUpTo)

        // 3. Current position dot with glow
        if (pointsUpTo < allPoints.size) {
            val current = allPoints[pointsUpTo]
            val (cx, cy) = gpsToScreen(current.latitude, current.longitude)
            canvas.drawCircle(cx, cy, 20f, dotGlowPaint)
            canvas.drawCircle(cx, cy, 10f, dotPaint)
        }

        // 4. Metrics overlay panel (bottom 35%)
        drawMetricsPanel(canvas, pointsUpTo)

        // 5. Photo overlay if present
        if (photoBitmap != null && photoAlpha > 0f) {
            drawPhoto(canvas, photoBitmap, photoAlpha)
        }
    }

    private fun drawRoute(canvas: Canvas, upToIndex: Int) {
        if (upToIndex < 1) return

        val path = Path()
        val (startX, startY) = gpsToScreen(allPoints[0].latitude, allPoints[0].longitude)
        path.moveTo(startX, startY)

        for (i in 1..upToIndex) {
            val (x, y) = gpsToScreen(allPoints[i].latitude, allPoints[i].longitude)
            path.lineTo(x, y)
        }

        // Draw shadow first
        val shadowPaint = Paint(linePaint).apply {
            color = Color.argb(80, 0, 0, 0)
            strokeWidth = config.lineWidthPx + 4f
        }
        canvas.save()
        canvas.translate(2f, 2f)
        canvas.drawPath(path, shadowPaint)
        canvas.restore()

        // Draw main line
        canvas.drawPath(path, linePaint)
    }

    private fun drawMetricsPanel(canvas: Canvas, currentPointIndex: Int) {
        val panelTop = config.height * 0.68f
        val panelRect = RectF(0f, panelTop, config.width.toFloat(), config.height.toFloat())
        canvas.drawRect(panelRect, overlayPaint)

        if (currentPointIndex >= allPoints.size) return
        val current = allPoints[currentPointIndex]

        // Calculate current metrics
        var distanceSoFar = 0f
        for (i in 1..currentPointIndex) {
            val prev = allPoints[i - 1]
            val curr = allPoints[i]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude,
                results
            )
            distanceSoFar += results[0]
        }

        val elapsedMs = current.timestampMs - startTimeMs
        val speedKmh = current.speed * 3.6f

        // Draw metrics
        val startX = 60f
        var y = panelTop + 70f

        drawMetric(canvas, "DISTANCIA", FormatUtils.formatDistance(distanceSoFar), startX, y)
        drawMetric(canvas, "VELOCIDAD", String.format("%.1f km/h", speedKmh), startX + 350f, y)

        y += 120f
        drawMetric(canvas, "TIEMPO", FormatUtils.formatDuration(elapsedMs), startX, y)
        drawMetric(canvas, "PUNTOS", "$currentPointIndex / ${allPoints.size}", startX + 350f, y)
    }

    private fun drawMetric(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x, y + 50f, textPaint)
    }

    private fun drawPhoto(canvas: Canvas, bitmap: Bitmap, alpha: Float) {
        val photoPaint = Paint().apply { this.alpha = (alpha * 255).toInt() }
        // Center photo in the map area with padding
        val maxW = config.width * 0.6f
        val maxH = config.height * 0.4f
        val scale = minOf(maxW / bitmap.width, maxH / bitmap.height)
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale
        val left = (config.width - scaledW) / 2
        val top = (config.height * 0.65f - scaledH) / 2

        // Draw photo shadow
        val shadowRect = RectF(left + 4, top + 4, left + scaledW + 4, top + scaledH + 4)
        val shadowPaint = Paint().apply {
            color = Color.argb((alpha * 100).toInt(), 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(shadowRect, 12f, 12f, shadowPaint)

        // Draw photo
        val destRect = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(bitmap, null, destRect, photoPaint)
    }

    /**
     * Maps GPS coordinates to screen pixel coordinates.
     * Latitude is inverted (higher lat = higher on screen = lower Y).
     */
    private fun gpsToScreen(lat: Double, lng: Double): Pair<Float, Float> {
        val latRange = maxLat - minLat
        val lngRange = maxLng - minLng

        val x = if (lngRange > 0) {
            mapPadding + ((lng - minLng) / lngRange * mapWidth).toFloat()
        } else {
            config.width / 2f
        }

        val y = if (latRange > 0) {
            mapPadding + ((maxLat - lat) / latRange * mapHeight).toFloat()
        } else {
            config.height * 0.325f
        }

        return Pair(x, y)
    }
}
