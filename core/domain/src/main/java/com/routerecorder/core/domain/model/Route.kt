package com.routerecorder.core.domain.model

/**
 * Represents a recorded route/trip.
 */
data class Route(
    val id: Long = 0,
    val name: String = "",
    val activityType: ActivityType = ActivityType.WALK,
    val trackingProfile: TrackingProfile = TrackingProfile.BALANCED,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0,
    val totalDistanceMeters: Float = 0f,
    val maxSpeedMs: Float = 0f,
    val avgSpeedMs: Float = 0f,
    val status: RouteStatus = RouteStatus.RECORDING,
    val thumbnailPath: String? = null,
    val videoPath: String? = null
) {
    /** Total duration in milliseconds */
    val durationMs: Long get() = endTimeMs - startTimeMs

    /** Total distance in kilometers */
    val distanceKm: Float get() = totalDistanceMeters / 1000f

    /** Average speed in km/h */
    val avgSpeedKmh: Float get() = avgSpeedMs * 3.6f

    /** Max speed in km/h */
    val maxSpeedKmh: Float get() = maxSpeedMs * 3.6f
}
