package com.test.playback.controller

import com.test.core.model.Track
import com.test.playback.model.RepeatMode
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {

    val state: StateFlow<PlaybackState>                          //## 재생 상태 스트림
    fun setQueueAndPlay(tracks: List<Track>, startTrackId: Long) //## 큐 세팅 후 지정 트랙부터 재생
    fun togglePlayPause()                                        //## 재생/일시정지 토글
    fun pause()                                                  //## 일시정지
    fun resume()                                                 //## 재생 재개
    fun next()                                                   //## 다음 곡
    fun previous()                                               //## 이전 곡(또는 처음으로)
    fun seekTo(positionMs: Long)                                 //## 지정 위치로 이동
    fun setRepeatMode(mode: RepeatMode)                          //## 반복 모드 설정
    fun setShuffleEnabled(enabled: Boolean)                      //## 셔플 설정
    fun stopAndReset()                                           //## 완전 종료(정지/초기화)
}
