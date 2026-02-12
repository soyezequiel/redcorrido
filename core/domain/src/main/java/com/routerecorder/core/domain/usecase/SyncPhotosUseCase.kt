package com.routerecorder.core.domain.usecase

import com.routerecorder.core.domain.model.RoutePhoto
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.repository.RoutePhotoRepository
import com.routerecorder.core.domain.repository.TrackPointRepository
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Synchronizes photos with the nearest track point using EXIF data.
 * Algorithm:
 * 1. If photo has lat/lng → find nearest point by Haversine distance
 * 2. If photo has only timestamp → find nearest point by temporal proximity
 * 3. If no metadata → cannot associate (returns null nearestPointId)
 */
class SyncPhotosUseCase @Inject constructor(
    private val trackPointRepository: TrackPointRepository,
    private val routePhotoRepository: RoutePhotoRepository
) {
    suspend operator fun invoke(routeId: Long, photos: List<RoutePhoto>) {
        val points = trackPointRepository.getFilteredPointsForRouteSync(routeId)
        if (points.isEmpty()) return

        for (photo in photos) {
            val insertedId = routePhotoRepository.insertPhoto(photo.copy(routeId = routeId))

            val nearestPoint = findNearestPoint(photo, points)
            if (nearestPoint != null) {
                val distance = if (photo.latitude != null && photo.longitude != null) {
                    haversineDistance(
                        photo.latitude, photo.longitude,
                        nearestPoint.latitude, nearestPoint.longitude
                    ).toFloat()
                } else {
                    0f
                }
                routePhotoRepository.updateNearestPoint(insertedId, nearestPoint.id, distance)
            }
        }
    }

    private fun findNearestPoint(photo: RoutePhoto, points: List<TrackPoint>): TrackPoint? {
        // Strategy 1: Geographic proximity
        if (photo.latitude != null && photo.longitude != null
            && photo.latitude != 0.0 && photo.longitude != 0.0
        ) {
            return points.minByOrNull { point ->
                haversineDistance(photo.latitude, photo.longitude, point.latitude, point.longitude)
            }
        }

        // Strategy 2: Temporal proximity
        if (photo.timestampMs > 0) {
            return points.minByOrNull { point ->
                abs(point.timestampMs - photo.timestampMs)
            }
        }

        // No metadata available
        return null
    }

    companion object {
        /** Haversine distance in meters between two lat/lng pairs */
        fun haversineDistance(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
