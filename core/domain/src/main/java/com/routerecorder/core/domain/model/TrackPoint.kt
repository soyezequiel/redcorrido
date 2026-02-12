package com.routerecorder.core.domain.model

/**
 * A single GPS data point recorded during a route.
 */
data class TrackPoint(
    val id: Long = 0,
    val routeId: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Float? = null,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val timestampMs: Long = 0,
    val isFiltered: Boolean = false
)
