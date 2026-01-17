package com.test.presentation.navigation

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{trackId}"

    fun detail(trackId: Long) = "detail/$trackId"
}
