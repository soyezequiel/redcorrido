package com.routerecorder.core.data.di

import android.content.Context
import androidx.room.Room
import com.routerecorder.core.data.local.RouteDatabase
import com.routerecorder.core.data.local.dao.RouteDao
import com.routerecorder.core.data.local.dao.RoutePhotoDao
import com.routerecorder.core.data.local.dao.TrackPointDao
import com.routerecorder.core.data.repository.RoutePhotoRepositoryImpl
import com.routerecorder.core.data.repository.RouteRepositoryImpl
import com.routerecorder.core.data.repository.TrackPointRepositoryImpl
import com.routerecorder.core.domain.repository.RoutePhotoRepository
import com.routerecorder.core.domain.repository.RouteRepository
import com.routerecorder.core.domain.repository.TrackPointRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RouteDatabase {
        return Room.databaseBuilder(
            context,
            RouteDatabase::class.java,
            RouteDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideRouteDao(database: RouteDatabase): RouteDao = database.routeDao()

    @Provides
    fun provideTrackPointDao(database: RouteDatabase): TrackPointDao = database.trackPointDao()

    @Provides
    fun provideRoutePhotoDao(database: RouteDatabase): RoutePhotoDao = database.routePhotoDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository

    @Binds
    @Singleton
    abstract fun bindTrackPointRepository(impl: TrackPointRepositoryImpl): TrackPointRepository

    @Binds
    @Singleton
    abstract fun bindRoutePhotoRepository(impl: RoutePhotoRepositoryImpl): RoutePhotoRepository
}
