package com.routerecorder.core.domain.usecase

import com.routerecorder.core.domain.model.ActivityType
import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.model.RouteStatus
import com.routerecorder.core.domain.model.TrackingProfile
import com.routerecorder.core.domain.repository.RouteRepository
import javax.inject.Inject

/**
 * Creates a new route and returns its ID.
 */
class StartRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(
        name: String,
        activityType: ActivityType,
        trackingProfile: TrackingProfile
    ): Long {
        val route = Route(
            name = name,
            activityType = activityType,
            trackingProfile = trackingProfile,
            startTimeMs = System.currentTimeMillis(),
            status = RouteStatus.RECORDING
        )
        return routeRepository.insertRoute(route)
    }
}
