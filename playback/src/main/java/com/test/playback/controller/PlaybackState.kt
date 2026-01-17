package com.test.playback.controller

import com.test.playback.model.RepeatMode

data class PlaybackState(
    val queueIds: List<Long> = emptyList(),      //## 현재 재생 큐(트랙 ID 목록)
    val currentTrackId: Long? = null,            //## 현재 재생 중인 트랙 ID
    val isPlaying: Boolean = false,              //## 재생 중 여부
    val positionMs: Long = 0L,                   //## 현재 재생 위치(ms)
    val durationMs: Long = 0L,                   //## 전체 길이(ms)
    val repeatMode: RepeatMode = RepeatMode.ALL, //## 반복 모드(ALL/ONE)
    val isShuffleEnabled: Boolean = false        //## 셔플 활성화 여부
)
