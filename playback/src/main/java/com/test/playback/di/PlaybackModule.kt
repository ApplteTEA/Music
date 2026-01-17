package com.test.playback.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.test.playback.controller.MediaPlaybackController
import com.test.playback.controller.PlaybackController
import com.test.playback.service.ForegroundServiceStarter
import com.test.playback.service.ForegroundServiceStarterImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context).build()

    @Provides
    @Singleton
    fun providePlayer(exoPlayer: ExoPlayer): Player = exoPlayer

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideForegroundServiceStarter(
        impl: ForegroundServiceStarterImpl
    ): ForegroundServiceStarter = impl

    @Provides
    @Singleton
    fun provideMediaPlaybackController(
        player: ExoPlayer,
        fgService: ForegroundServiceStarter
    ): MediaPlaybackController = MediaPlaybackController(player, fgService)

    @Provides
    @Singleton
    fun providePlaybackController(
        controller: MediaPlaybackController
    ): PlaybackController = controller
}
