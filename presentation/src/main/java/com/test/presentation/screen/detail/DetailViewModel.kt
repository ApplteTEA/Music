package com.test.presentation.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.core.model.Track
import com.test.domain.repository.TrackRepository
import com.test.playback.controller.PlaybackController
import com.test.playback.model.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playbackController: PlaybackController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val requestedTrackId: Long = checkNotNull(savedStateHandle.get<Long>("trackId"))

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var latestTracks: List<Track> = emptyList()

    private var tracksJob: Job? = null
    private var playbackJob: Job? = null

    private var lockToRequested = true

    init {
        observeTracks()
        observePlayback()
    }

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            DetailUiEvent.OnNextClick,
            DetailUiEvent.OnPrevClick,
            DetailUiEvent.OnPlayPauseClick -> lockToRequested = false
            else -> Unit
        }

        when (event) {
            DetailUiEvent.OnPlayPauseClick -> {
                val showId = playbackController.state.value.currentTrackId ?: requestedTrackId
                val tracks = latestTracks

                val queueEmpty = playbackController.state.value.queueIds.isEmpty()
                if (!uiState.value.isPlaying && queueEmpty && tracks.isNotEmpty()) {
                    playbackController.setQueueAndPlay(tracks, showId)
                    return
                }

                playbackController.togglePlayPause()
            }

            DetailUiEvent.OnNextClick -> playbackController.next()
            DetailUiEvent.OnPrevClick -> playbackController.previous()

            DetailUiEvent.OnToggleShuffle -> {
                val next = !_uiState.value.isShuffleEnabled
                playbackController.setShuffleEnabled(next)
                _uiState.update { it.copy(isShuffleEnabled = next) }
            }

            is DetailUiEvent.OnSeekTo -> playbackController.seekTo(event.positionMs)

            DetailUiEvent.OnToggleRepeat -> {
                val next = when (_uiState.value.repeatMode) {
                    RepeatMode.ONE -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                }
                playbackController.setRepeatMode(next)
                _uiState.update { it.copy(repeatMode = next) }
            }
        }
    }

    /**
     * 트랙 목록을 구독해서 requested/current 트랙 정보를 UI에 반영한다.
     * ---------------------------------------------------------------------------------------------
     *
     */
    private fun observeTracks() {
        if (tracksJob != null) return

        tracksJob = viewModelScope.launch {
            trackRepository.getTracks().collectLatest { tracks ->
                latestTracks = tracks

                val showId = resolveShowId()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        track = tracks.firstOrNull { t -> t.id == showId }
                    )
                }
            }
        }
    }

    /**
     * 재생 상태(현재곡/재생여부/포지션/반복/셔플)를 구독해서 UI에 반영한다.
     * ---------------------------------------------------------------------------------------------
     *
     */
    private fun observePlayback() {
        if (playbackJob != null) return

        playbackJob = viewModelScope.launch {
            playbackController.state.collectLatest { s ->
                if (lockToRequested && s.currentTrackId == requestedTrackId) {
                    lockToRequested = false
                }

                val showId = resolveShowId(currentId = s.currentTrackId)
                val showTrack = latestTracks.firstOrNull { it.id == showId }

                _uiState.update {
                    it.copy(
                        track = showTrack ?: it.track,
                        isPlaying = s.isPlaying,
                        positionMs = s.positionMs,
                        durationMs = s.durationMs,
                        repeatMode = s.repeatMode,
                        isShuffleEnabled = s.isShuffleEnabled
                    )
                }
            }
        }
    }

    /**
     * 디테일 화면에서 보여줄 트랙 ID를 결정한다.
     * ---------------------------------------------------------------------------------------------
     *
     */
    private fun resolveShowId(currentId: Long? = playbackController.state.value.currentTrackId): Long {
        if (lockToRequested) return requestedTrackId
        return currentId ?: requestedTrackId
    }
}
