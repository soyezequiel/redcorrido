package com.routerecorder.core.domain.model

/** Type of activity being tracked */
enum class ActivityType {
    WALK,
    BIKE,
    CAR
}

/** GPS tracking power consumption profile */
enum class TrackingProfile(
    val intervalMs: Long,
    val fastestIntervalMs: Long,
    val minDisplacementMeters: Float,
    val stillnessTimeoutMs: Long,
    val maxAccuracyMeters: Float,
    val maxSpeedKmh: Float
) {
    HIGH_ACCURACY(
        intervalMs = 2_000,
        fastestIntervalMs = 1_000,
        minDisplacementMeters = 2f,
        stillnessTimeoutMs = 60_000,
        maxAccuracyMeters = 30f,
        maxSpeedKmh = 60f
    ),
    BALANCED(
        intervalMs = 5_000,
        fastestIntervalMs = 3_000,
        minDisplacementMeters = 5f,
        stillnessTimeoutMs = 120_000,
        maxAccuracyMeters = 50f,
        maxSpeedKmh = 150f
    ),
    LOW_POWER(
        intervalMs = 15_000,
        fastestIntervalMs = 10_000,
        minDisplacementMeters = 20f,
        stillnessTimeoutMs = 300_000,
        maxAccuracyMeters = 100f,
        maxSpeedKmh = 300f
    )
}

/** Current state of a route */
enum class RouteStatus {
    RECORDING,
    PAUSED,
    COMPLETED
}

/** State of the tracking service */
enum class TrackingState {
    IDLE,
    TRACKING,
    PAUSED,
    AUTO_PAUSED
}
