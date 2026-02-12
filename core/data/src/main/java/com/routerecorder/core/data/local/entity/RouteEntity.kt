package com.routerecorder.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val activityType: String = "WALK",
    val trackingProfile: String = "BALANCED",
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0,
    val totalDistanceMeters: Float = 0f,
    val maxSpeedMs: Float = 0f,
    val avgSpeedMs: Float = 0f,
    val status: String = "RECORDING",
    val thumbnailPath: String? = null,
    val videoPath: String? = null
)
