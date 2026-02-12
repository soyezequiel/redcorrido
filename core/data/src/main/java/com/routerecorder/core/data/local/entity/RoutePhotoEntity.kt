package com.routerecorder.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_photos",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routeId"])
    ]
)
data class RoutePhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: Long = 0,
    val nearestPointId: Long? = null,
    val filePath: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestampMs: Long = 0,
    val distanceToNearestPointMeters: Float = 0f
)
