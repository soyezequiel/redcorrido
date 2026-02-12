package com.routerecorder.core.domain.repository

import com.routerecorder.core.domain.model.RoutePhoto
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for RoutePhoto operations.
 */
interface RoutePhotoRepository {

    fun getPhotosForRoute(routeId: Long): Flow<List<RoutePhoto>>

    suspend fun getPhotosForRouteSync(routeId: Long): List<RoutePhoto>

    suspend fun insertPhoto(photo: RoutePhoto): Long

    suspend fun insertPhotos(photos: List<RoutePhoto>)

    suspend fun deletePhoto(photoId: Long)

    suspend fun updateNearestPoint(photoId: Long, nearestPointId: Long, distanceMeters: Float)
}
