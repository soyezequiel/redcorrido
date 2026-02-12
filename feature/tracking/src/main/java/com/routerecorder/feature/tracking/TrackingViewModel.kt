package com.routerecorder.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routerecorder.core.domain.model.ActivityType
import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.model.TrackingMetrics
import com.routerecorder.core.domain.model.TrackingProfile
import com.routerecorder.core.domain.model.TrackingState
import com.routerecorder.core.domain.repository.TrackPointRepository
import com.routerecorder.core.domain.usecase.FinishRouteUseCase
import com.routerecorder.core.domain.usecase.StartRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val isTracking: Boolean = false,
    val trackingState: TrackingState = TrackingState.IDLE,
    val selectedProfile: TrackingProfile = TrackingProfile.BALANCED,
    val selectedActivity: ActivityType = ActivityType.WALK,
    val currentRouteId: Long? = null,
    val metrics: TrackingMetrics = TrackingMetrics(),
    val trackPoints: List<TrackPoint> = emptyList(),
    val showProfileSelector: Boolean = true,
    val routeName: String = ""
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val startRouteUseCase: StartRouteUseCase,
    private val finishRouteUseCase: FinishRouteUseCase,
    private val trackPointRepository: TrackPointRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    fun selectProfile(profile: TrackingProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }

    fun selectActivity(activity: ActivityType) {
        _uiState.update { it.copy(selectedActivity = activity) }
    }

    fun updateRouteName(name: String) {
        _uiState.update { it.copy(routeName = name) }
    }

    fun startTracking() {
        viewModelScope.launch {
            val name = _uiState.value.routeName.ifEmpty {
                "Recorrido ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())}"
            }
            val routeId = startRouteUseCase(
                name = name,
                activityType = _uiState.value.selectedActivity,
                trackingProfile = _uiState.value.selectedProfile
            )
            _uiState.update {
                it.copy(
                    isTracking = true,
                    trackingState = TrackingState.TRACKING,
                    currentRouteId = routeId,
                    showProfileSelector = false
                )
            }

            // Observe track points for live map
            launch {
                trackPointRepository.getFilteredPointsForRoute(routeId).collect { points ->
                    _uiState.update { it.copy(trackPoints = points) }
                }
            }
        }
    }

    fun pauseTracking() {
        _uiState.update { it.copy(trackingState = TrackingState.PAUSED) }
    }

    fun resumeTracking() {
        _uiState.update { it.copy(trackingState = TrackingState.TRACKING) }
    }

    fun stopTracking() {
        viewModelScope.launch {
            val routeId = _uiState.value.currentRouteId ?: return@launch
            finishRouteUseCase(routeId)
            _uiState.update {
                it.copy(
                    isTracking = false,
                    trackingState = TrackingState.IDLE,
                    showProfileSelector = true,
                    trackPoints = emptyList()
                )
            }
        }
    }

    fun updateMetrics(metrics: TrackingMetrics) {
        _uiState.update { it.copy(metrics = metrics) }
    }
}
