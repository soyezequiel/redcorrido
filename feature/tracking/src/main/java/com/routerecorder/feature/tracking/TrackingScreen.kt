package com.routerecorder.feature.tracking

import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.routerecorder.core.common.util.FormatUtils
import com.routerecorder.core.domain.model.ActivityType
import com.routerecorder.core.domain.model.TrackingState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel,
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Activity Type Selector
        Text(
            text = "Tipo de actividad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ActivityType.values().forEachIndexed { index, type ->
                SegmentedButton(
                    selected = uiState.selectedActivity == type,
                    onClick = { viewModel.selectActivity(type) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ActivityType.values().size
                    ),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (type) {
                                    ActivityType.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                                    ActivityType.BIKE -> Icons.AutoMirrored.Filled.DirectionsBike
                                    ActivityType.CAR -> Icons.Default.DirectionsCar
                                },
                                contentDescription = type.name,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when (type) {
                                    ActivityType.WALK -> "Caminar"
                                    ActivityType.BIKE -> "Bici"
                                    ActivityType.CAR -> "Auto"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Metrics Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Métricas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem("Distancia", FormatUtils.formatDistance(uiState.metrics.totalDistanceMeters))
                    MetricItem("Duración", FormatUtils.formatDuration(uiState.metrics.elapsedTimeMs))
                    MetricItem("Velocidad", FormatUtils.formatSpeed(uiState.metrics.currentSpeedMs))
                }
            }
        }

        // OSMDroid Map
        val context = LocalContext.current
        val mapView = remember {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                // Default to Buenos Aires
                controller.setCenter(GeoPoint(-34.6037, -58.3816))
            }
        }

        // Add "my location" overlay
        val locationOverlay = remember {
            MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                enableMyLocation()
                enableFollowLocation()
                mapView.overlays.add(this)
            }
        }

        // Route polyline overlay
        val routePolyline = remember {
            Polyline(mapView).apply {
                outlinePaint.color = Color.parseColor("#2196F3")
                outlinePaint.strokeWidth = 10f
                mapView.overlays.add(this)
            }
        }

        // Update polyline when trackPoints change
        val trackPoints = uiState.trackPoints
        if (trackPoints.isNotEmpty()) {
            val geoPoints = trackPoints.map { GeoPoint(it.latitude, it.longitude) }
            routePolyline.setPoints(geoPoints)
            mapView.controller.animateTo(geoPoints.last())
            mapView.invalidate()
        }

        DisposableEffect(Unit) {
            mapView.onResume()
            onDispose {
                locationOverlay.disableMyLocation()
                locationOverlay.disableFollowLocation()
                mapView.onPause()
                mapView.onDetach()
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (uiState.trackingState) {
                TrackingState.IDLE -> {
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
                }
                TrackingState.TRACKING -> {
                    FloatingActionButton(
                        onClick = { viewModel.pauseTracking() },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pausar")
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    FloatingActionButton(
                        onClick = {
                            viewModel.stopTracking()
                            onNavigateToHistory()
                        },
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Detener")
                    }
                }
                TrackingState.PAUSED, TrackingState.AUTO_PAUSED -> {
                    FloatingActionButton(
                        onClick = { viewModel.resumeTracking() },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reanudar")
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    FloatingActionButton(
                        onClick = {
                            viewModel.stopTracking()
                            onNavigateToHistory()
                        },
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Detener")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
