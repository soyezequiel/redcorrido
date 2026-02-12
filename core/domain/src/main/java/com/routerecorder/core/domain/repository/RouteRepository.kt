package com.routerecorder.core.domain.repository

import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.model.RouteStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Route CRUD operations.
 * Implemented in :core:data, consumed by use cases.
 */
interface RouteRepository {

    fun getAllRoutes(): Flow<List<Route>>

    fun getRouteById(routeId: Long): Flow<Route?>

    fun getRoutesByStatus(status: RouteStatus): Flow<List<Route>>

    suspend fun insertRoute(route: Route): Long

    suspend fun updateRoute(route: Route)

    suspend fun deleteRoute(routeId: Long)

    suspend fun updateRouteMetrics(
        routeId: Long,
        totalDistanceMeters: Float,
        avgSpeedMs: Float,
        maxSpeedMs: Float,
        endTimeMs: Long
    )

    suspend fun updateRouteStatus(routeId: Long, status: RouteStatus)

    suspend fun updateVideoPath(routeId: Long, videoPath: String)
}
