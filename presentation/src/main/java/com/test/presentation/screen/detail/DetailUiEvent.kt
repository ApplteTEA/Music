package com.test.presentation.screen.detail

sealed interface DetailUiEvent {
    data object OnPlayPauseClick : DetailUiEvent              //## 재생/일시정지 토글
    data object OnNextClick : DetailUiEvent                   //## 다음 곡
    data object OnPrevClick : DetailUiEvent                   //## 이전 곡(또는 처음으로)
    data class OnSeekTo(val positionMs: Long) : DetailUiEvent //## 특정 위치로 이동(ms)
    data object OnToggleRepeat : DetailUiEvent                //## 반복 모드 토글(ALL <-> ONE)
    data object OnToggleShuffle : DetailUiEvent               //## 셔플 토글
}
