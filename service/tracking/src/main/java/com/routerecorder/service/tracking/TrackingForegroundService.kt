package com.routerecorder.service.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.routerecorder.core.domain.model.TrackingProfile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingForegroundService : Service() {

    @Inject
    lateinit var locationTracker: LocationTracker

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.routerecorder.action.START_TRACKING"
        const val ACTION_PAUSE = "com.routerecorder.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.routerecorder.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.routerecorder.action.STOP_TRACKING"

        const val EXTRA_ROUTE_ID = "extra_route_id"
        const val EXTRA_PROFILE = "extra_profile"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startTracking(context: Context, routeId: Long, profile: TrackingProfile) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ROUTE_ID, routeId)
                putExtra(EXTRA_PROFILE, profile.name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseTracking(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeTracking(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val routeId = intent.getLongExtra(EXTRA_ROUTE_ID, -1)
                val profileName = intent.getStringExtra(EXTRA_PROFILE) ?: "BALANCED"
                val profile = TrackingProfile.valueOf(profileName)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification("Grabando recorrido..."),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification("Grabando recorrido..."))
                }
                acquireWakeLock()
                _isRunning.value = true

                serviceScope.launch {
                    locationTracker.startTracking(routeId, profile)
                }
            }
            ACTION_PAUSE -> {
                serviceScope.launch {
                    locationTracker.pauseTracking()
                }
                updateNotification("Recorrido pausado")
            }
            ACTION_RESUME -> {
                serviceScope.launch {
                    locationTracker.resumeTracking()
                }
                updateNotification("Grabando recorrido...")
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    locationTracker.stopTracking()
                }
                releaseWakeLock()
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
        _isRunning.value = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tracking de recorridos",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación del servicio de tracking GPS"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RouteRecorder")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RouteRecorder::TrackingWakeLock"
        ).apply {
            acquire(12 * 60 * 60 * 1000L) // Max 12 hours
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}
