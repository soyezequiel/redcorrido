package com.routerecorder.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.routerecorder.core.data.local.dao.RouteDao
import com.routerecorder.core.data.local.dao.TrackPointDao
import com.routerecorder.core.data.local.dao.RoutePhotoDao
import com.routerecorder.core.data.local.entity.RouteEntity
import com.routerecorder.core.data.local.entity.TrackPointEntity
import com.routerecorder.core.data.local.entity.RoutePhotoEntity

@Database(
    entities = [
        RouteEntity::class,
        TrackPointEntity::class,
        RoutePhotoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun routePhotoDao(): RoutePhotoDao

    companion object {
        const val DATABASE_NAME = "route_recorder.db"
    }
}
