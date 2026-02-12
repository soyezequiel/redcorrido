package com.routerecorder.core.common.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formatting utilities for display of metrics.
 */
object FormatUtils {

    /** Format meters to km with 2 decimal places, or meters if < 1km */
    fun formatDistance(meters: Float): String {
        return if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.2f km", meters / 1000f)
        } else {
            String.format(Locale.getDefault(), "%.0f m", meters)
        }
    }

    /** Format m/s to km/h with 1 decimal place */
    fun formatSpeed(speedMs: Float): String {
        val kmh = speedMs * 3.6f
        return String.format(Locale.getDefault(), "%.1f km/h", kmh)
    }

    /** Format duration from milliseconds to HH:MM:SS */
    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /** Format timestamp to readable time */
    fun formatTime(timestampMs: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(java.util.Date(timestampMs))
    }

    /** Format timestamp to readable date */
    fun formatDate(timestampMs: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(timestampMs))
    }
}
