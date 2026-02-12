package com.routerecorder.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.routerecorder.core.data.local.entity.RoutePhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutePhotoDao {

    @Query("SELECT * FROM route_photos WHERE routeId = :routeId ORDER BY timestampMs ASC")
    fun getPhotosForRoute(routeId: Long): Flow<List<RoutePhotoEntity>>

    @Query("SELECT * FROM route_photos WHERE routeId = :routeId ORDER BY timestampMs ASC")
    suspend fun getPhotosForRouteSync(routeId: Long): List<RoutePhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: RoutePhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<RoutePhotoEntity>)

    @Query("DELETE FROM route_photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: Long)

    @Query("""
        UPDATE route_photos SET 
            nearestPointId = :nearestPointId,
            distanceToNearestPointMeters = :distanceMeters
        WHERE id = :photoId
    """)
    suspend fun updateNearestPoint(photoId: Long, nearestPointId: Long, distanceMeters: Float)
}
