package com.routerecorder.core.data.repository

import com.routerecorder.core.data.local.dao.RoutePhotoDao
import com.routerecorder.core.data.mapper.toDomain
import com.routerecorder.core.data.mapper.toEntity
import com.routerecorder.core.domain.model.RoutePhoto
import com.routerecorder.core.domain.repository.RoutePhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutePhotoRepositoryImpl @Inject constructor(
    private val routePhotoDao: RoutePhotoDao
) : RoutePhotoRepository {

    override fun getPhotosForRoute(routeId: Long): Flow<List<RoutePhoto>> =
        routePhotoDao.getPhotosForRoute(routeId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPhotosForRouteSync(routeId: Long): List<RoutePhoto> =
        routePhotoDao.getPhotosForRouteSync(routeId).map { it.toDomain() }

    override suspend fun insertPhoto(photo: RoutePhoto): Long =
        routePhotoDao.insertPhoto(photo.toEntity())

    override suspend fun insertPhotos(photos: List<RoutePhoto>) =
        routePhotoDao.insertPhotos(photos.map { it.toEntity() })

    override suspend fun deletePhoto(photoId: Long) =
        routePhotoDao.deletePhoto(photoId)

    override suspend fun updateNearestPoint(photoId: Long, nearestPointId: Long, distanceMeters: Float) =
        routePhotoDao.updateNearestPoint(photoId, nearestPointId, distanceMeters)
}
