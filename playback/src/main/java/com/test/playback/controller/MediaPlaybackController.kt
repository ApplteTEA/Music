package com.test.playback.controller

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.test.core.model.Track
import com.test.playback.model.RepeatMode
import com.test.playback.service.ForegroundServiceStarter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaPlaybackController @Inject constructor(
    private val player: ExoPlayer,
    private val fgService: ForegroundServiceStarter
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var progressJob: Job? = null

    private var lastQueueTracks: List<Track> = emptyList()

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL

        player.addListener(object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                update {
                    it.copy(
                        isPlaying = isPlaying,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                if (isPlaying) startProgress() else stopProgress()
                fgService.invalidate()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                update { it.copy(durationMs = player.duration.coerceAtLeast(0L)) }
                fgService.invalidate()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId?.toLongOrNull()
                update {
                    it.copy(
                        currentTrackId = id,
                        positionMs = 0L,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                fgService.invalidate()
            }
        })
    }

    private fun ensureServiceRunning() {
        fgService.start()
    }

    override fun setQueueAndPlay(tracks: List<Track>, startTrackId: Long) {
        ensureServiceRunning()

        lastQueueTracks = tracks

        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(Uri.parse(track.contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }

        val startIndex = tracks.indexOfFirst { it.id == startTrackId }.let { if (it >= 0) it else 0 }

        player.repeatMode = when (_state.value.repeatMode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        player.shuffleModeEnabled = _state.value.isShuffleEnabled

        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.play()

        update {
            it.copy(
                queueIds = tracks.map { t -> t.id },
                currentTrackId = tracks.getOrNull(startIndex)?.id,
                isPlaying = true,
                positionMs = 0L,
                durationMs = player.duration.coerceAtLeast(0L),
                repeatMode = it.repeatMode,
                isShuffleEnabled = it.isShuffleEnabled
            )
        }

        startProgress()
        fgService.invalidate()
    }

    override fun togglePlayPause() {
        if (player.isPlaying) pause() else resume()
    }

    override fun pause() {
        player.pause()
        update { it.copy(isPlaying = false) }
        stopProgress()

        fgService.invalidate()
    }

    override fun resume() {
        ensureServiceRunning()

        if (player.mediaItemCount == 0 || _state.value.queueIds.isEmpty()) {
            val fallbackTracks = lastQueueTracks
            val wantedId =
                _state.value.currentTrackId
                    ?: player.currentMediaItem?.mediaId?.toLongOrNull()
                    ?: fallbackTracks.firstOrNull()?.id

            if (fallbackTracks.isNotEmpty() && wantedId != null) {
                setQueueAndPlay(fallbackTracks, wantedId)
                return
            }
        }

        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekToDefaultPosition()
        }

        player.play()
        update { it.copy(isPlaying = true, durationMs = player.duration.coerceAtLeast(0L)) }
        startProgress()
        fgService.invalidate()
    }

    override fun next() {
        ensureServiceRunning()
        player.seekToNextMediaItem()
        player.play()
        fgService.invalidate()
    }

    override fun previous() {
        ensureServiceRunning()
        val pos = player.currentPosition
        if (pos > 3_000L) player.seekTo(0L) else player.seekToPreviousMediaItem()
        player.play()
        fgService.invalidate()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0L)) }
        fgService.invalidate()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        player.repeatMode = when (mode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        update { it.copy(repeatMode = mode) }
        fgService.invalidate()
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
        update { it.copy(isShuffleEnabled = enabled) }
        fgService.invalidate()
    }

    override fun stopAndReset() {
        player.pause()
        player.seekTo(0L)
        player.clearMediaItems()

        stopProgress()

        lastQueueTracks = emptyList()

        update {
            PlaybackState(
                queueIds = emptyList(),
                currentTrackId = null,
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L,
                repeatMode = RepeatMode.ALL,
                isShuffleEnabled = false
            )
        }

        fgService.stop(removeNotification = true)
    }

    /**
     * 재생 진행률(현재 위치/전체 길이)을 주기적으로 갱신하는 Job 시작
     * ---------------------------------------------------------------------------------------------
     *
     * 이미 Job이 있으면 중복 실행하지 않음
     * 500ms 간격으로 player.currentPosition / player.duration을 state에 반영
     */
    private fun startProgress() {
        if (progressJob != null) return
        progressJob = scope.launch {
            while (isActive) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                _state.update { it.copy(positionMs = pos, durationMs = dur) }
                delay(500L)
            }
        }
    }

    /**
     * 진행률 갱신 Job 중지
     * ---------------------------------------------------------------------------------------------
     *
     * ViewModel/Service 생명주기나 일시정지 시 불필요한 갱신을 막기 위해 취소
     */
    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * PlaybackState 단일 진입점 업데이트 헬퍼
     * ---------------------------------------------------------------------------------------------
     *
     * 현재 state를 기반으로 copy하여 갱신하는 패턴을 통일
     */
    private fun update(playback: (PlaybackState) -> PlaybackState) {
        _state.value = playback(_state.value)
    }
}
