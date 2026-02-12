package com.routerecorder.core.domain.model

/**
 * Real-time metrics emitted during tracking.
 */
data class TrackingMetrics(
    val totalDistanceMeters: Float = 0f,
    val elapsedTimeMs: Long = 0,
    val currentSpeedMs: Float = 0f,
    val avgSpeedMs: Float = 0f,
    val maxSpeedMs: Float = 0f,
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val pointCount: Int = 0,
    val trackingState: TrackingState = TrackingState.IDLE
) {
    val distanceKm: Float get() = totalDistanceMeters / 1000f
    val currentSpeedKmh: Float get() = currentSpeedMs * 3.6f
    val avgSpeedKmh: Float get() = avgSpeedMs * 3.6f
    val maxSpeedKmh: Float get() = maxSpeedMs * 3.6f
}
