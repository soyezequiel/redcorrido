package com.routerecorder.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routerecorder.core.domain.model.Route
import com.routerecorder.core.domain.repository.RouteRepository
import com.routerecorder.core.domain.usecase.GetRoutesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val routes: List<Route> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getRoutesUseCase: GetRoutesUseCase,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getRoutesUseCase().collect { routes ->
                _uiState.value = HistoryUiState(routes = routes, isLoading = false)
            }
        }
    }

    fun deleteRoute(routeId: Long) {
        viewModelScope.launch {
            routeRepository.deleteRoute(routeId)
        }
    }
}
