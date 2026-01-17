package com.test.presentation.screen.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.core.model.Track
import com.test.domain.repository.TrackRepository
import com.test.playback.controller.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ListViewModel @Inject constructor(
    private val trackRepository: TrackRepository,      //## 트랙 목록 조회
    private val playbackController: PlaybackController //## 재생 제어 + 현재 재생 상태 관찰
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ListUiEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var tracksJob: Job? = null
    private var playbackJob: Job? = null

    private var latestTracks: List<Track> = emptyList()
    private var currentTrackIdCache: Long? = null

    fun onEvent(event: ListUiEvent) {
        when (event) {
            ListUiEvent.PermissionGranted -> {
                observeTracks()
                observeNowPlaying()
            }

            ListUiEvent.RetryLoad -> restartObserveTracks()

            is ListUiEvent.ClickTrack -> {
                val tracks = latestTracks
                if (tracks.isEmpty()) return

                playbackController.setQueueAndPlay(tracks, event.trackId)

                viewModelScope.launch {
                    _effect.send(ListUiEffect.NavigateDetail(event.trackId))
                }
            }

            ListUiEvent.OnNowPlayingClick -> {
                val id = currentTrackIdCache ?: return
                viewModelScope.launch {
                    _effect.send(ListUiEffect.NavigateDetail(id))
                }
            }
        }
    }

    /**
     * 목록 스트림을 초기화 후 재구독해서 최신 상태로 갱신한다.
     * ---------------------------------------------------------------------------------------------
     *
     * 기존 collect Job을 끊고
     * 로딩 상태로 돌린 뒤
     * observeTracks()를 다시 시작한다.
     */
    private fun restartObserveTracks() {
        tracksJob?.cancel()
        tracksJob = null
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeTracks()
    }

    /**
     * MediaStore(Repository)에서 트랙 목록 Flow를 구독한다.
     * ---------------------------------------------------------------------------------------------
     *
     * 새로운 목록이 들어오면 latestTracks를 갱신하고
     * 현재 재생중인 ID가 있으면 nowPlayingTrack도 같이 찾아서 세팅한다.
     */
    private fun observeTracks() {
        if (tracksJob != null) return

        tracksJob = viewModelScope.launch {
            trackRepository.getTracks().collectLatest { tracks ->
                latestTracks = tracks

                val nowId = currentTrackIdCache
                val nowTrack = nowId?.let { id -> tracks.firstOrNull { it.id == id } }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tracks = tracks,
                        errorMessage = null,
                        nowPlayingTrack = nowTrack ?: it.nowPlayingTrack
                    )
                }
            }
        }
    }

    /**
     * PlaybackController의 재생 상태를 구독한다.
     * ---------------------------------------------------------------------------------------------
     *
     * currentTrackId 변화만 추적해서(distinct)
     * 리스트 재생중 표시와 하단 NowPlayingBar 표시 정보를 갱신한다.
     */
    private fun observeNowPlaying() {
        if (playbackJob != null) return

        playbackJob = viewModelScope.launch {
            playbackController.state
                .map { it.currentTrackId }
                .distinctUntilChanged()
                .collectLatest { currentId ->
                    currentTrackIdCache = currentId

                    val nowTrack = currentId?.let { id ->
                        latestTracks.firstOrNull { it.id == id }
                    }

                    _uiState.update {
                        it.copy(
                            currentTrackId = currentId,
                            nowPlayingTrack = nowTrack
                        )
                    }
                }
        }
    }

}
