package com.test.data.di

import com.test.data.repository.MediaStoreTrackRepository
import com.test.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataRepositoryModule {

    @Binds
    fun bindTrackRepository(
        impl: MediaStoreTrackRepository
    ): TrackRepository
}
