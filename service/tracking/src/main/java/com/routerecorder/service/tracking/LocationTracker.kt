package com.routerecorder.service.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.routerecorder.core.common.location.LocationFilterPipeline
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.model.TrackingMetrics
import com.routerecorder.core.domain.model.TrackingProfile
import com.routerecorder.core.domain.model.TrackingState
import com.routerecorder.core.domain.repository.TrackPointRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core GPS tracking engine.
 * Manages Fused Location Provider, applies filter pipeline,
 * buffers points for batch insert, and emits real-time metrics.
 */
@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackPointRepository: TrackPointRepository
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentRouteId: Long = -1
    private var currentProfile: TrackingProfile = TrackingProfile.BALANCED
    private var filterPipeline: LocationFilterPipeline? = null

    // Point buffer for batch inserts
    private val pointBuffer = mutableListOf<TrackPoint>()
    private val bufferMaxSize = 50

    // Metrics accumulation
    private var totalDistanceMeters = 0f
    private var maxSpeedMs = 0f
    private var speedSum = 0f
    private var pointCount = 0
    private var lastLocation: Location? = null
    private var trackingStartTimeMs = 0L

    // Stillness detection
    private var stillSinceMs: Long? = null

    private val _metrics = MutableStateFlow(TrackingMetrics())
    val metrics: StateFlow<TrackingMetrics> = _metrics.asStateFlow()

    private val _trackingState = MutableStateFlow(TrackingState.IDLE)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                onNewLocation(location)
            }
        }
    }

    fun startTracking(routeId: Long, profile: TrackingProfile) {
        currentRouteId = routeId
        currentProfile = profile
        filterPipeline = LocationFilterPipeline(
            maxAccuracyMeters = profile.maxAccuracyMeters,
            maxSpeedKmh = profile.maxSpeedKmh
        )

        // Reset accumulators
        totalDistanceMeters = 0f
        maxSpeedMs = 0f
        speedSum = 0f
        pointCount = 0
        lastLocation = null
        trackingStartTimeMs = System.currentTimeMillis()
        stillSinceMs = null
        pointBuffer.clear()

        _trackingState.value = TrackingState.TRACKING
        requestLocationUpdates()
    }

    fun pauseTracking() {
        _trackingState.value = TrackingState.PAUSED
        fusedClient.removeLocationUpdates(locationCallback)
        flushBuffer()
    }

    fun resumeTracking() {
        _trackingState.value = TrackingState.TRACKING
        stillSinceMs = null
        requestLocationUpdates()
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        flushBuffer()
        _trackingState.value = TrackingState.IDLE
        filterPipeline?.reset()
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val priority = when (currentProfile) {
            TrackingProfile.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
            TrackingProfile.BALANCED -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            TrackingProfile.LOW_POWER -> Priority.PRIORITY_LOW_POWER
        }

        val request = LocationRequest.Builder(priority, currentProfile.intervalMs)
            .setMinUpdateIntervalMillis(currentProfile.fastestIntervalMs)
            .setMinUpdateDistanceMeters(currentProfile.minDisplacementMeters)
            .setWaitForAccurateLocation(currentProfile == TrackingProfile.HIGH_ACCURACY)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun onNewLocation(rawLocation: Location) {
        if (_trackingState.value != TrackingState.TRACKING) return

        // Apply filter pipeline
        val filteredLocation = filterPipeline?.process(rawLocation) ?: return

        // Stillness detection
        if (filteredLocation.speed < 0.5f) {
            if (stillSinceMs == null) {
                stillSinceMs = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - stillSinceMs!! > currentProfile.stillnessTimeoutMs) {
                _trackingState.value = TrackingState.AUTO_PAUSED
                fusedClient.removeLocationUpdates(locationCallback)
                flushBuffer()
                return
            }
        } else {
            stillSinceMs = null
            // Resume from auto-pause if we were auto-paused
            if (_trackingState.value == TrackingState.AUTO_PAUSED) {
                _trackingState.value = TrackingState.TRACKING
            }
        }

        // Calculate distance delta
        lastLocation?.let { last ->
            val delta = filteredLocation.distanceTo(last)
            totalDistanceMeters += delta
        }

        // Update speed metrics
        if (filteredLocation.speed > maxSpeedMs) {
            maxSpeedMs = filteredLocation.speed
        }
        speedSum += filteredLocation.speed
        pointCount++

        lastLocation = filteredLocation

        // Create track point
        val trackPoint = TrackPoint(
            routeId = currentRouteId,
            latitude = filteredLocation.latitude,
            longitude = filteredLocation.longitude,
            altitude = if (filteredLocation.hasAltitude()) filteredLocation.altitude.toFloat() else null,
            accuracy = filteredLocation.accuracy,
            speed = filteredLocation.speed,
            bearing = filteredLocation.bearing,
            timestampMs = filteredLocation.time,
            isFiltered = false
        )

        // Buffer for batch insert
        synchronized(pointBuffer) {
            pointBuffer.add(trackPoint)
            if (pointBuffer.size >= bufferMaxSize) {
                flushBuffer()
            }
        }

        // Emit updated metrics
        val avgSpeed = if (pointCount > 0) speedSum / pointCount else 0f
        _metrics.value = TrackingMetrics(
            totalDistanceMeters = totalDistanceMeters,
            elapsedTimeMs = System.currentTimeMillis() - trackingStartTimeMs,
            currentSpeedMs = filteredLocation.speed,
            avgSpeedMs = avgSpeed,
            maxSpeedMs = maxSpeedMs,
            currentLatitude = filteredLocation.latitude,
            currentLongitude = filteredLocation.longitude,
            pointCount = pointCount,
            trackingState = _trackingState.value
        )
    }

    private fun flushBuffer() {
        synchronized(pointBuffer) {
            if (pointBuffer.isEmpty()) return
            val toInsert = pointBuffer.toList()
            pointBuffer.clear()
            scope.launch {
                trackPointRepository.insertPoints(toInsert)
            }
        }
    }
}
