package com.test.presentation.screen.detail

import com.test.core.model.Track
import com.test.playback.model.RepeatMode

data class DetailUiState(
    val isLoading: Boolean = false,              //## 로딩 상태(트랙/재생정보 준비 중)
    val track: Track? = null,                    //## 화면에 표시할 현재 트랙
    val isPlaying: Boolean = false,              //## 재생 중 여부
    val positionMs: Long = 0L,                   //## 현재 재생 위치(ms)
    val durationMs: Long = 0L,                   //## 총 길이(ms)
    val repeatMode: RepeatMode = RepeatMode.ALL, //## 반복 모드(ALL / ONE)
    val isShuffleEnabled: Boolean = false        //## 셔플 활성화 여부
)
