package com.routerecorder.core.common.location

import android.location.Location
import com.routerecorder.core.common.location.KalmanLocationFilter

/**
 * Pipeline of filters applied to raw GPS locations before storage.
 * Filters are applied in order — each can reject a location or pass it through.
 */
class LocationFilterPipeline(
    private val maxAccuracyMeters: Float,
    private val maxSpeedKmh: Float
) {
    private val kalmanFilter = KalmanLocationFilter()
    private var lastAcceptedLocation: Location? = null

    /**
     * Process a raw location through the filter pipeline.
     * @return the filtered location, or null if the point should be rejected
     */
    fun process(rawLocation: Location): Location? {
        // Filter 1: Accuracy threshold
        if (rawLocation.accuracy > maxAccuracyMeters) {
            return null
        }

        // Filter 2: Impossible speed check
        val lastLoc = lastAcceptedLocation
        if (lastLoc != null) {
            val timeDeltaSec = (rawLocation.time - lastLoc.time) / 1000.0
            if (timeDeltaSec > 0) {
                val distanceM = rawLocation.distanceTo(lastLoc)
                val impliedSpeedKmh = (distanceM / timeDeltaSec) * 3.6
                if (impliedSpeedKmh > maxSpeedKmh) {
                    return null // Teleportation detected, reject
                }
            }
        }

        // Filter 3: Kalman smoothing
        val smoothed = kalmanFilter.filter(rawLocation)

        lastAcceptedLocation = smoothed
        return smoothed
    }

    /** Reset all filter state for a new tracking session */
    fun reset() {
        kalmanFilter.reset()
        lastAcceptedLocation = null
    }
}
