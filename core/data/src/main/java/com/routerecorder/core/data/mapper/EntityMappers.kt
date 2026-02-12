package com.routerecorder.core.data.mapper

import com.routerecorder.core.data.local.entity.RouteEntity
import com.routerecorder.core.data.local.entity.RoutePhotoEntity
import com.routerecorder.core.data.local.entity.TrackPointEntity
import com.routerecorder.core.domain.model.ActivityType
import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.model.RoutePhoto
import com.routerecorder.core.domain.model.RouteStatus
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.model.TrackingProfile

// ── Route ──

fun RouteEntity.toDomain(): Route = Route(
    id = id,
    name = name,
    activityType = ActivityType.valueOf(activityType),
    trackingProfile = TrackingProfile.valueOf(trackingProfile),
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    totalDistanceMeters = totalDistanceMeters,
    maxSpeedMs = maxSpeedMs,
    avgSpeedMs = avgSpeedMs,
    status = RouteStatus.valueOf(status),
    thumbnailPath = thumbnailPath,
    videoPath = videoPath
)

fun Route.toEntity(): RouteEntity = RouteEntity(
    id = id,
    name = name,
    activityType = activityType.name,
    trackingProfile = trackingProfile.name,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    totalDistanceMeters = totalDistanceMeters,
    maxSpeedMs = maxSpeedMs,
    avgSpeedMs = avgSpeedMs,
    status = status.name,
    thumbnailPath = thumbnailPath,
    videoPath = videoPath
)

// ── TrackPoint ──

fun TrackPointEntity.toDomain(): TrackPoint = TrackPoint(
    id = id,
    routeId = routeId,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    accuracy = accuracy,
    speed = speed,
    bearing = bearing,
    timestampMs = timestampMs,
    isFiltered = isFiltered
)

fun TrackPoint.toEntity(): TrackPointEntity = TrackPointEntity(
    id = id,
    routeId = routeId,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    accuracy = accuracy,
    speed = speed,
    bearing = bearing,
    timestampMs = timestampMs,
    isFiltered = isFiltered
)

// ── RoutePhoto ──

fun RoutePhotoEntity.toDomain(): RoutePhoto = RoutePhoto(
    id = id,
    routeId = routeId,
    nearestPointId = nearestPointId,
    filePath = filePath,
    latitude = latitude,
    longitude = longitude,
    timestampMs = timestampMs,
    distanceToNearestPointMeters = distanceToNearestPointMeters
)

fun RoutePhoto.toEntity(): RoutePhotoEntity = RoutePhotoEntity(
    id = id,
    routeId = routeId,
    nearestPointId = nearestPointId,
    filePath = filePath,
    latitude = latitude,
    longitude = longitude,
    timestampMs = timestampMs,
    distanceToNearestPointMeters = distanceToNearestPointMeters
)
