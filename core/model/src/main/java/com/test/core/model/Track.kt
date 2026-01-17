package com.test.core.model

data class Track(
    val id: Long,            //## 고유 ID
    val title: String,       //## 제목
    val artist: String,      //## 아티스트
    val album: String,       //## 앨범명
    val durationMs: Long,    //## 재생 시간(ms)
    val contentUri: String,  //## 오디오 파일 URI
    val albumArtUri: String? //## 앨범아트 URI(없을 수 있음)
)
