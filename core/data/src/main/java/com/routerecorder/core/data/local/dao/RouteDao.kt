package com.routerecorder.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.routerecorder.core.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY startTimeMs DESC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun getRouteById(routeId: Long): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE status = :status ORDER BY startTimeMs DESC")
    fun getRoutesByStatus(status: String): Flow<List<RouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: Long)

    @Query("""
        UPDATE routes SET 
            totalDistanceMeters = :totalDistanceMeters,
            avgSpeedMs = :avgSpeedMs,
            maxSpeedMs = :maxSpeedMs,
            endTimeMs = :endTimeMs
        WHERE id = :routeId
    """)
    suspend fun updateRouteMetrics(
        routeId: Long,
        totalDistanceMeters: Float,
        avgSpeedMs: Float,
        maxSpeedMs: Float,
        endTimeMs: Long
    )

    @Query("UPDATE routes SET status = :status WHERE id = :routeId")
    suspend fun updateRouteStatus(routeId: Long, status: String)

    @Query("UPDATE routes SET videoPath = :videoPath WHERE id = :routeId")
    suspend fun updateVideoPath(routeId: Long, videoPath: String)
}
