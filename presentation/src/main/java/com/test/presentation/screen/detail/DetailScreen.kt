package com.test.presentation.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.test.playback.model.RepeatMode
import com.test.presentation.util.hSpace
import com.test.presentation.util.space
import com.test.presentation.util.wSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onEvent: (DetailUiEvent) -> Unit
) {
    val track = state.track

    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(
        0L,
        if (duration > 0L) duration else Long.MAX_VALUE
    )

    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(position.toFloat()) }

    LaunchedEffect(position, duration) {
        if (!isSeeking) seekValue = position.toFloat()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            8.wSpace()

            Text(
                text = track?.album ?: "재생 중",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val shuffleTint =
                if (state.isShuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant

            IconButton(onClick = { onEvent(DetailUiEvent.OnToggleShuffle) }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = shuffleTint
                )
            }
        }

        24.hSpace()

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val art = track?.albumArtUri
            if (art.isNullOrBlank()) {
                Text("No Album Art", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AsyncImage(
                    model = art,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        20.hSpace()

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = track?.title ?: "제목 없음",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            6.hSpace()

            Text(
                text = track?.artist ?: "아티스트 없음",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            20.hSpace()

            val sliderEnabled = duration > 0L
            val maxSlider = if (sliderEnabled) duration.toFloat() else 1f

            val trackHeight = 8.dp
            val thumbSize = 12.dp

            val sliderInteraction = remember { MutableInteractionSource() }

            Slider(
                value = if (sliderEnabled) seekValue.coerceIn(0f, maxSlider) else 0f,
                onValueChange = {
                    if (!sliderEnabled) return@Slider
                    isSeeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    if (!sliderEnabled) return@Slider
                    isSeeking = false
                    onEvent(DetailUiEvent.OnSeekTo(seekValue.toLong()))
                },
                valueRange = 0f..maxSlider,
                enabled = sliderEnabled,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = sliderInteraction,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .indication(sliderInteraction, null)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(trackHeight)
                    )
                }
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatMs(if (isSeeking) seekValue.toLong() else position),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(formatMs(duration), style = MaterialTheme.typography.labelMedium)
            }

            18.hSpace()

            val repeatIcon = when (state.repeatMode) {
                RepeatMode.ONE -> Icons.Filled.RepeatOne
                RepeatMode.ALL -> Icons.Filled.Repeat
            }
            val repeatTint = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onEvent(DetailUiEvent.OnToggleRepeat) }) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatTint
                    )
                }

                IconButton(onClick = { onEvent(DetailUiEvent.OnPrevClick) }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev")
                }

                FilledIconButton(
                    onClick = { onEvent(DetailUiEvent.OnPlayPauseClick) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "PlayPause",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { onEvent(DetailUiEvent.OnNextClick) }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }

                48.wSpace()
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).toInt().coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
