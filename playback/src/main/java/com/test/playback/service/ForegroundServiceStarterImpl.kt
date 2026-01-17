package com.test.playback.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@UnstableApi
class ForegroundServiceStarterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ForegroundServiceStarter {

    override fun start() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MusicPlaybackService::class.java).apply {
                    action = MusicPlaybackService.ACTION_START
                }
            )
        }
    }

    override fun invalidate() {
        runCatching {
            context.startService(
                Intent(context, MusicPlaybackService::class.java).apply {
                    action = MusicPlaybackService.ACTION_INVALIDATE
                }
            )
        }
    }

    override fun stop(removeNotification: Boolean) {
        runCatching {
            context.startService(
                Intent(context, MusicPlaybackService::class.java).apply {
                    action = MusicPlaybackService.ACTION_STOP
                    putExtra(MusicPlaybackService.EXTRA_REMOVE_NOTIFICATION, removeNotification)
                }
            )
        }
    }
}
