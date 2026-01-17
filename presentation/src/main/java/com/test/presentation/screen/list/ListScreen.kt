package com.test.presentation.screen.list

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.test.core.model.Track
import com.test.presentation.util.AudioPermissionStatus
import com.test.presentation.util.hSpace
import com.test.presentation.util.rememberAudioPermissionState
import com.test.presentation.util.space

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ListScreen(
    onOpenDetail: (trackId: Long) -> Unit,
    viewModel: ListViewModel = hiltViewModel()
) {
    val audioPermission = rememberAudioPermissionState(requestOnEnter = true)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { eff ->
            when (eff) {
                is ListUiEffect.NavigateDetail -> onOpenDetail(eff.trackId)
            }
        }
    }

    when (audioPermission.status) {
        AudioPermissionStatus.GRANTED -> {
            LaunchedEffect(Unit) { viewModel.onEvent(ListUiEvent.PermissionGranted) }

            val state by viewModel.uiState.collectAsStateWithLifecycle()

            Scaffold(
                bottomBar = {
                    NowPlayingBar(
                        track = state.nowPlayingTrack,
                        onClick = { viewModel.onEvent(ListUiEvent.OnNowPlayingClick) }
                    )
                }
            ) { _ ->
                Column(Modifier.fillMaxSize()) {
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )

                    when {
                        state.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        state.errorMessage != null -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("오류: ${state.errorMessage}")
                                    12.hSpace()
                                    Button(onClick = { viewModel.onEvent(ListUiEvent.RetryLoad) }) {
                                        Text("다시 시도")
                                    }
                                }
                            }
                        }

                        state.tracks.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("기기에 저장된 음악이 없어요.")
                            }
                        }

                        else -> {
                            TrackList(
                                tracks = state.tracks,
                                currentTrackId = state.currentTrackId,
                                hasNowPlaying = (state.nowPlayingTrack != null),
                                onClick = { track ->
                                    viewModel.onEvent(ListUiEvent.ClickTrack(track.id))
                                }
                            )
                        }
                    }
                }
            }
        }

        AudioPermissionStatus.DENIED -> {
            PermissionDeniedScreen(onRequestAgain = { audioPermission.request() })
        }

        AudioPermissionStatus.PERMANENTLY_DENIED -> {
            PermissionPermanentlyDeniedScreen(onOpenSettings = { audioPermission.openSettings() })
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onRequestAgain: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("음악 접근 권한이 필요해요")
            8.hSpace()
            Text("기기에 저장된 음악을 불러오려면 권한을 허용해야 해요.")
            20.hSpace()
            Button(onClick = onRequestAgain) { Text("권한 허용하기") }
        }
    }
}

@Composable
private fun PermissionPermanentlyDeniedScreen(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("권한이 꺼져 있어요")
            8.hSpace()
            Text("‘다시 묻지 않음’으로 거절된 상태라 설정에서 직접 켜야 해요.")
            20.hSpace()
            Button(onClick = onOpenSettings) { Text("설정으로 이동") }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    currentTrackId: Long?,
    hasNowPlaying: Boolean,
    onClick: (Track) -> Unit
) {
    //## NowPlayingBar가 있을 때만 하단 여백 확보
    val bottomSafePadding = if (hasNowPlaying) 80.dp else 12.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = bottomSafePadding
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            count = tracks.size,
            key = { idx -> tracks[idx].id }
        ) { idx ->
            val track = tracks[idx]
            TrackRow(
                track = track,
                isCurrent = (track.id == currentTrackId),
                onClick = { onClick(track) }
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        12.space()

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title.ifBlank { "(No Title)" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isCurrent) {
                    8.space()
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Text(
                text = track.artist.ifBlank { "(Unknown Artist)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NowPlayingBar(
    track: Track?,
    onClick: () -> Unit
) {
    if (track == null) return

    val bg = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        10.space()

        Column(Modifier.weight(1f)) {
            Text(
                text = track.title.ifBlank { "(No Title)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist.ifBlank { "(Unknown Artist)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
