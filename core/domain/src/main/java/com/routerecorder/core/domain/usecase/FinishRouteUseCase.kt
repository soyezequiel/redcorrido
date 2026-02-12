package com.routerecorder.core.domain.usecase

import com.routerecorder.core.domain.model.RouteStatus
import com.routerecorder.core.domain.repository.RouteRepository
import com.routerecorder.core.domain.repository.TrackPointRepository
import javax.inject.Inject

/**
 * Finalizes an active route: updates status, calculates final metrics.
 */
class FinishRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
    private val trackPointRepository: TrackPointRepository
) {
    suspend operator fun invoke(routeId: Long) {
        val points = trackPointRepository.getFilteredPointsForRouteSync(routeId)
        if (points.isEmpty()) return

        var totalDistance = 0f
        var maxSpeed = 0f
        var speedSum = 0f

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude,
                results
            )
            totalDistance += results[0]
            if (curr.speed > maxSpeed) maxSpeed = curr.speed
            speedSum += curr.speed
        }

        val avgSpeed = if (points.size > 1) speedSum / (points.size - 1) else 0f
        val endTime = points.last().timestampMs

        routeRepository.updateRouteMetrics(
            routeId = routeId,
            totalDistanceMeters = totalDistance,
            avgSpeedMs = avgSpeed,
            maxSpeedMs = maxSpeed,
            endTimeMs = endTime
        )
        routeRepository.updateRouteStatus(routeId, RouteStatus.COMPLETED)
    }
}
