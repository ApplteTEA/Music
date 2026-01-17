package com.test.presentation.screen.list

sealed interface ListUiEvent {
    data object PermissionGranted : ListUiEvent            //## 오디오 권한 허용됨(트랙 조회/재생 상태 관찰 시작)
    data object RetryLoad : ListUiEvent                    //## 트랙 목록 다시 불러오기
    data class ClickTrack(val trackId: Long) : ListUiEvent //## 리스트 아이템 클릭(재생 + 디테일 이동)
    data object OnNowPlayingClick : ListUiEvent            //## NowPlayingBar 클릭(현재 재생 트랙 디테일 이동)
}
