package com.test.presentation.screen.list

import com.test.core.model.Track

data class ListUiState(
    val isLoading: Boolean = true,          //## 로딩 중 여부
    val tracks: List<Track> = emptyList(),  //## 기기에서 조회한 트랙 목록
    val errorMessage: String? = null,       //## 로딩 실패 시 에러 메시지
    val currentTrackId: Long? = null,       //## 현재 재생 중인 트랙 ID(리스트 표시용)
    val nowPlayingTrack: Track? = null      //## 하단 NowPlayingBar에 표시할 트랙(정보용)
)
