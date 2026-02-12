package com.routerecorder.core.data.repository

import com.routerecorder.core.data.local.dao.RouteDao
import com.routerecorder.core.data.mapper.toDomain
import com.routerecorder.core.data.mapper.toEntity
import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.model.RouteStatus
import com.routerecorder.core.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao
) : RouteRepository {

    override fun getAllRoutes(): Flow<List<Route>> =
        routeDao.getAllRoutes().map { entities -> entities.map { it.toDomain() } }

    override fun getRouteById(routeId: Long): Flow<Route?> =
        routeDao.getRouteById(routeId).map { it?.toDomain() }

    override fun getRoutesByStatus(status: RouteStatus): Flow<List<Route>> =
        routeDao.getRoutesByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertRoute(route: Route): Long =
        routeDao.insertRoute(route.toEntity())

    override suspend fun updateRoute(route: Route) =
        routeDao.updateRoute(route.toEntity())

    override suspend fun deleteRoute(routeId: Long) =
        routeDao.deleteRoute(routeId)

    override suspend fun updateRouteMetrics(
        routeId: Long,
        totalDistanceMeters: Float,
        avgSpeedMs: Float,
        maxSpeedMs: Float,
        endTimeMs: Long
    ) = routeDao.updateRouteMetrics(routeId, totalDistanceMeters, avgSpeedMs, maxSpeedMs, endTimeMs)

    override suspend fun updateRouteStatus(routeId: Long, status: RouteStatus) =
        routeDao.updateRouteStatus(routeId, status.name)

    override suspend fun updateVideoPath(routeId: Long, videoPath: String) =
        routeDao.updateVideoPath(routeId, videoPath)
}
