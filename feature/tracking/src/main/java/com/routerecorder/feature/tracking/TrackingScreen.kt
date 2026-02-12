package com.routerecorder.feature.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routerecorder.core.common.util.FormatUtils
import com.routerecorder.core.domain.model.ActivityType
import com.routerecorder.core.domain.model.TrackingProfile
import com.routerecorder.core.domain.model.TrackingState

@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Profile & Activity selector (only before tracking)
        AnimatedVisibility(visible = state.showProfileSelector) {
            Column {
                Text(
                    text = "Nuevo Recorrido",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.routeName,
                    onValueChange = { viewModel.updateRouteName(it) },
                    label = { Text("Nombre del recorrido (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Activity type selector
                Text(
                    text = "Tipo de actividad",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActivityChip(
                        icon = Icons.Default.DirectionsWalk,
                        label = "Caminar",
                        selected = state.selectedActivity == ActivityType.WALK,
                        onClick = { viewModel.selectActivity(ActivityType.WALK) },
                        modifier = Modifier.weight(1f)
                    )
                    ActivityChip(
                        icon = Icons.Default.DirectionsBike,
                        label = "Bicicleta",
                        selected = state.selectedActivity == ActivityType.BIKE,
                        onClick = { viewModel.selectActivity(ActivityType.BIKE) },
                        modifier = Modifier.weight(1f)
                    )
                    ActivityChip(
                        icon = Icons.Default.DirectionsCar,
                        label = "Auto",
                        selected = state.selectedActivity == ActivityType.CAR,
                        onClick = { viewModel.selectActivity(ActivityType.CAR) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Profile selector
                Text(
                    text = "Perfil de consumo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProfileSelector(
                    selected = state.selectedProfile,
                    onProfileSelected = { viewModel.selectProfile(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Map placeholder (to be replaced with Mapbox/OSMDroid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            if (!state.isTracking) {
                Text(
                    text = "El mapa se mostrará aquí\ncuando inicies un recorrido",
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            } else {
                // TODO: Integrate Mapbox/OSMDroid MapView with live polyline
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📍 Grabando...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.trackPoints.size} puntos",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics panel (visible during tracking)
        AnimatedVisibility(visible = state.isTracking) {
            MetricsPanel(state)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!state.isTracking) {
                FloatingActionButton(
                    onClick = { viewModel.startTracking() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Iniciar",
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                // Pause/Resume
                FloatingActionButton(
                    onClick = {
                        when (state.trackingState) {
                            TrackingState.TRACKING -> viewModel.pauseTracking()
                            TrackingState.PAUSED, TrackingState.AUTO_PAUSED -> viewModel.resumeTracking()
                            else -> {}
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (state.trackingState == TrackingState.TRACKING)
                            Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pausar/Reanudar"
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Stop
                FloatingActionButton(
                    onClick = { viewModel.stopTracking() },
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Detener",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActivityChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "chipBg"
    )

    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfileSelector(
    selected: TrackingProfile,
    onProfileSelected: (TrackingProfile) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrackingProfile.values().forEach { profile ->
            val isSelected = profile == selected
            val label = when (profile) {
                TrackingProfile.HIGH_ACCURACY -> "Alta precisión"
                TrackingProfile.BALANCED -> "Balanceado"
                TrackingProfile.LOW_POWER -> "Bajo consumo"
            }
            val desc = when (profile) {
                TrackingProfile.HIGH_ACCURACY -> "Caminar / Bici"
                TrackingProfile.BALANCED -> "Uso general"
                TrackingProfile.LOW_POWER -> "Viajes largos"
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileSelected(profile) }
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        ) else Modifier
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = desc,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsPanel(state: TrackingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricItem(
                label = "Distancia",
                value = FormatUtils.formatDistance(state.metrics.totalDistanceMeters)
            )
            MetricItem(
                label = "Velocidad",
                value = FormatUtils.formatSpeed(state.metrics.currentSpeedMs)
            )
            MetricItem(
                label = "Tiempo",
                value = FormatUtils.formatDuration(state.metrics.elapsedTimeMs)
            )
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
