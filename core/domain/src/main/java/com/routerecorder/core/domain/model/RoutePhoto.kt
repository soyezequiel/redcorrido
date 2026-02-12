package com.routerecorder.core.domain.model

/**
 * A photo associated with a route, containing geolocation metadata.
 */
data class RoutePhoto(
    val id: Long = 0,
    val routeId: Long = 0,
    val nearestPointId: Long? = null,
    val filePath: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestampMs: Long = 0,
    val distanceToNearestPointMeters: Float = 0f
)
