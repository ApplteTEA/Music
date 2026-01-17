package com.test.presentation.screen.list

sealed interface ListUiEffect {
    data class NavigateDetail(val trackId: Long) : ListUiEffect //## 디테일 화면으로 네비게이션
}
