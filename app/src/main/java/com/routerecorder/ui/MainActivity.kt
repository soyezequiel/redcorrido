package com.routerecorder.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.routerecorder.feature.history.HistoryScreen
import com.routerecorder.feature.history.HistoryViewModel
import com.routerecorder.feature.tracking.TrackingScreen
import com.routerecorder.feature.tracking.TrackingViewModel
import com.routerecorder.feature.video.VideoScreen
import com.routerecorder.feature.video.VideoViewModel
import com.routerecorder.ui.theme.RouteRecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RouteRecorderTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var videoRouteId by remember { mutableIntStateOf(-1) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = "Tracking") },
                    label = { Text("Recorrido") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("Historial") }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> {
                    val vm: TrackingViewModel = hiltViewModel()
                    TrackingScreen(
                        viewModel = vm,
                        onNavigateToHistory = { selectedTab = 1 }
                    )
                }
                1 -> {
                    if (videoRouteId > 0) {
                        val vm: VideoViewModel = hiltViewModel()
                        VideoScreen(
                            viewModel = vm,
                            routeId = videoRouteId.toLong(),
                            onVideoReady = { videoRouteId = -1 }
                        )
                    } else {
                        val vm: HistoryViewModel = hiltViewModel()
                        HistoryScreen(
                            viewModel = vm,
                            onRouteClick = { /* TODO: detail screen */ },
                            onGenerateVideo = { routeId ->
                                videoRouteId = routeId.toInt()
                            }
                        )
                    }
                }
            }
        }
    }
}
