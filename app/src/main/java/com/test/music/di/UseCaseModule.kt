package com.test.music.di

import com.test.domain.repository.TrackRepository
import com.test.domain.usecase.GetTracksUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetTracksUseCase(
        repository: TrackRepository
    ): GetTracksUseCase = GetTracksUseCase(repository)
}
