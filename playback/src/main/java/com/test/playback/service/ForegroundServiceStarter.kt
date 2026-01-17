package com.test.playback.service

interface ForegroundServiceStarter {
    fun start()                           //## 포그라운드 서비스 시작(재생 시작/재개 시)
    fun invalidate()                      //## 노티 상태 갱신 요청(서비스를 새로 띄우지 않는 용도)
    fun stop(removeNotification: Boolean) //## 서비스 정지(노티 제거 여부 선택)
}
