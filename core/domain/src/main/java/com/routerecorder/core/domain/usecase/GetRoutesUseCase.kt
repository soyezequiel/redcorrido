package com.routerecorder.core.domain.usecase

import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves completed routes for the history screen.
 */
class GetRoutesUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {
    operator fun invoke(): Flow<List<Route>> {
        return routeRepository.getAllRoutes()
    }
}
