package com.routerecorder.core.common.location

import android.location.Location

/**
 * Simplified Kalman filter for GPS location smoothing.
 *
 * Uses accuracy-weighted blending: points with higher accuracy (lower accuracy value)
 * get more weight. At low speeds, more smoothing is applied to reduce jitter.
 * At high speeds, the raw point is trusted more to preserve trajectory fidelity.
 */
class KalmanLocationFilter {

    private var estimatedLat: Double = 0.0
    private var estimatedLng: Double = 0.0
    private var variance: Float = Float.MAX_VALUE

    private var isInitialized = false

    /**
     * Filters a raw GPS location and returns a smoothed location.
     * The original location object is not modified.
     */
    fun filter(rawLocation: Location): Location {
        if (!isInitialized) {
            estimatedLat = rawLocation.latitude
            estimatedLng = rawLocation.longitude
            variance = rawLocation.accuracy * rawLocation.accuracy
            isInitialized = true
            return rawLocation
        }

        // Kalman gain: how much we trust the new measurement vs our estimate
        val measurementVariance = rawLocation.accuracy * rawLocation.accuracy
        val kalmanGain = variance / (variance + measurementVariance)

        // Update estimate
        estimatedLat += kalmanGain * (rawLocation.latitude - estimatedLat)
        estimatedLng += kalmanGain * (rawLocation.longitude - estimatedLng)

        // Update variance (our confidence improves)
        variance *= (1 - kalmanGain)

        // Increase variance over time to account for movement uncertainty
        // This prevents the filter from becoming too "sticky"
        val timeFactor = 1.0f + (rawLocation.speed * 0.1f)
        variance *= timeFactor

        return Location(rawLocation).apply {
            latitude = estimatedLat
            longitude = estimatedLng
        }
    }

    /** Reset the filter state for a new tracking session */
    fun reset() {
        estimatedLat = 0.0
        estimatedLng = 0.0
        variance = Float.MAX_VALUE
        isInitialized = false
    }
}
