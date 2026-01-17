package com.test.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.test.core.model.Track
import com.test.domain.repository.TrackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreTrackRepository @Inject constructor(
    @ApplicationContext context: Context
) : TrackRepository {

    private val resolver: ContentResolver = context.contentResolver

    override fun getTracks(): Flow<List<Track>> = callbackFlow {
        fun load(): List<Track> = runCatching {
            queryTracks()
        }.getOrDefault(emptyList())

        trySend(load())

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(load())
            }
        }

        resolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        awaitClose {
            resolver.unregisterContentObserver(observer)
        }
    }

    /**
     * 기기(MediaStore)에서 오디오 트랙 목록 조회
     * ---------------------------------------------------------------------------------------------
     *
     * IS_MUSIC 조건으로 음악 파일만 필터링
     * 최신 추가 순(DATE_ADDED DESC)으로 정렬
     *
     */
    private fun queryTracks(): List<Track> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val result = mutableListOf<Track>()

        resolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol).orEmpty()
                val artist = cursor.getString(artistCol).orEmpty()
                val album = cursor.getString(albumCol).orEmpty()
                val durationMs = cursor.getLong(durationCol)
                val albumId = cursor.getLong(albumIdCol)

                val contentUri = ContentUris.withAppendedId(uri, id)

                val titleFromFileName = displayName.removeFileExtension().ifBlank { "(No Title)" }

                val albumArtUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    albumId
                )

                result += Track(
                    id = id,
                    title = titleFromFileName,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    contentUri = contentUri.toString(),
                    albumArtUri = albumArtUri.toString()
                )
            }
        }

        return result
    }

    /**
     * 파일명에서 확장자 제거
     * ---------------------------------------------------------------------------------------------
     *
     * test.mp3 -> test
     * 확장자 구분자(.)가 없으면 원본 그대로 반환
     *
     */
    private fun String.removeFileExtension(): String {
        val dot = lastIndexOf('.')
        return if (dot > 0) substring(0, dot) else this
    }
}
