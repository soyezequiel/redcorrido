package com.routerecorder.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.routerecorder.core.data.local.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {

    @Query("SELECT * FROM track_points WHERE routeId = :routeId ORDER BY timestampMs ASC")
    fun getPointsForRoute(routeId: Long): Flow<List<TrackPointEntity>>

    @Query("""
        SELECT * FROM track_points 
        WHERE routeId = :routeId AND isFiltered = 0 
        ORDER BY timestampMs ASC
    """)
    fun getFilteredPointsForRoute(routeId: Long): Flow<List<TrackPointEntity>>

    @Query("""
        SELECT * FROM track_points 
        WHERE routeId = :routeId AND isFiltered = 0 
        ORDER BY timestampMs ASC
    """)
    suspend fun getFilteredPointsForRouteSync(routeId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE routeId = :routeId ORDER BY timestampMs ASC")
    suspend fun getPointsForRouteSync(routeId: Long): List<TrackPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("SELECT COUNT(*) FROM track_points WHERE routeId = :routeId")
    suspend fun getPointCount(routeId: Long): Int

    /**
     * Paginated query for video rendering — avoids loading all points into memory.
     */
    @Query("""
        SELECT * FROM track_points 
        WHERE routeId = :routeId AND isFiltered = 0 
        ORDER BY timestampMs ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPointsPaginated(routeId: Long, limit: Int, offset: Int): List<TrackPointEntity>

    @Query("UPDATE track_points SET isFiltered = 1 WHERE id IN (:pointIds)")
    suspend fun markPointsAsFiltered(pointIds: List<Long>)

    @Query("DELETE FROM track_points WHERE routeId = :routeId AND isFiltered = 1")
    suspend fun deleteFilteredPoints(routeId: Long)
}
