package com.routerecorder.core.data.repository

import com.routerecorder.core.data.local.dao.TrackPointDao
import com.routerecorder.core.data.mapper.toDomain
import com.routerecorder.core.data.mapper.toEntity
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.repository.TrackPointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackPointRepositoryImpl @Inject constructor(
    private val trackPointDao: TrackPointDao
) : TrackPointRepository {

    override fun getPointsForRoute(routeId: Long): Flow<List<TrackPoint>> =
        trackPointDao.getPointsForRoute(routeId).map { entities -> entities.map { it.toDomain() } }

    override fun getFilteredPointsForRoute(routeId: Long): Flow<List<TrackPoint>> =
        trackPointDao.getFilteredPointsForRoute(routeId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertPoint(point: TrackPoint): Long =
        trackPointDao.insertPoint(point.toEntity())

    override suspend fun insertPoints(points: List<TrackPoint>) =
        trackPointDao.insertPoints(points.map { it.toEntity() })

    override suspend fun getPointCount(routeId: Long): Int =
        trackPointDao.getPointCount(routeId)

    override suspend fun getPointsForRouteSync(routeId: Long): List<TrackPoint> =
        trackPointDao.getPointsForRouteSync(routeId).map { it.toDomain() }

    override suspend fun getFilteredPointsForRouteSync(routeId: Long): List<TrackPoint> =
        trackPointDao.getFilteredPointsForRouteSync(routeId).map { it.toDomain() }

    override suspend fun getPointsPaginated(routeId: Long, limit: Int, offset: Int): List<TrackPoint> =
        trackPointDao.getPointsPaginated(routeId, limit, offset).map { it.toDomain() }

    override suspend fun markPointsAsFiltered(pointIds: List<Long>) =
        trackPointDao.markPointsAsFiltered(pointIds)

    override suspend fun deleteFilteredPoints(routeId: Long) =
        trackPointDao.deleteFilteredPoints(routeId)
}
