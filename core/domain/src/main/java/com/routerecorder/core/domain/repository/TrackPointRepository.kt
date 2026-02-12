package com.routerecorder.core.domain.repository

import com.routerecorder.core.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for TrackPoint operations.
 */
interface TrackPointRepository {

    fun getPointsForRoute(routeId: Long): Flow<List<TrackPoint>>

    fun getFilteredPointsForRoute(routeId: Long): Flow<List<TrackPoint>>

    suspend fun insertPoint(point: TrackPoint): Long

    suspend fun insertPoints(points: List<TrackPoint>)

    suspend fun getPointCount(routeId: Long): Int

    suspend fun getPointsForRouteSync(routeId: Long): List<TrackPoint>

    suspend fun getFilteredPointsForRouteSync(routeId: Long): List<TrackPoint>

    /**
     * Paginated access to track points for video rendering.
     * Returns [limit] points starting from [offset], ordered by timestamp.
     */
    suspend fun getPointsPaginated(routeId: Long, limit: Int, offset: Int): List<TrackPoint>

    suspend fun markPointsAsFiltered(pointIds: List<Long>)

    suspend fun deleteFilteredPoints(routeId: Long)
}
