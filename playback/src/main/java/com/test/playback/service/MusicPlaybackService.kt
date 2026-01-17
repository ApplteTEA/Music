package com.test.playback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.target.Target
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTI_ID = 1001

        const val EXTRA_REMOVE_NOTIFICATION = "extra_remove_notification"

        const val ACTION_OPEN_DETAIL_FROM_NOTIFICATION =
            "com.test.playback.action.OPEN_DETAIL_FROM_NOTIFICATION"
        const val EXTRA_TRACK_ID = "extra_track_id"

        const val ACTION_START = "com.test.playback.action.START"
        const val ACTION_INVALIDATE = "com.test.playback.action.INVALIDATE"
        const val ACTION_STOP = "com.test.playback.action.STOP"
    }

    //## Hilt 주입 Player
    @Inject lateinit var player: Player

    //## NotificationManager 캐시
    private val nm by lazy { getSystemService(NotificationManager::class.java) }

    //## Media3 MediaSession
    private var mediaSession: MediaSession? = null

    //## Media3 PlayerNotificationManage
    private var notiManager: PlayerNotificationManager? = null

    //## 현재 startForeground 상태인지 추적
    private var isInForeground = false

    //## 앨범아트 로딩용 Coil ImageLoader
    private val imageLoader by lazy { ImageLoader(this) }

    //## Prev/Next 연타 에러 방지
    private val artworkGeneration = AtomicLong(0L)

    override fun onCreate() {
        super.onCreate()
        ensureChannel()

        mediaSession = MediaSession.Builder(this, player).build()

        //## PlayerNotificationManager 생성: 알림 UI/액션/큰 아이콘(앨범아트) 제공
        notiManager = PlayerNotificationManager.Builder(this, NOTI_ID, CHANNEL_ID)
            .setMediaDescriptionAdapter(descriptionAdapter)
            .setNotificationListener(notificationListener)
            .build()
            .apply {
                //### Prev/Next/PlayPause 유지
                setUsePreviousAction(true)
                setUseNextAction(true)
                setUsePlayPauseActions(true)

                //### 빨리감기/되감기 제거
                setUseFastForwardAction(false)
                setUseRewindAction(false)

                setUseStopAction(false)

                //### Prev/Next가 가끔 사라지는 케이스 방어
                setUsePreviousActionInCompactView(true)
                setUseNextActionInCompactView(true)

                //### 세션 토큰 연결
                runCatching {
                    val token = mediaSession?.sessionCompatToken
                    if (token != null) setMediaSessionToken(token)
                }

                //## Player 연결
                setPlayer(player)
            }

        player.addListener(playerListener)
    }

    override fun onDestroy() {
        player.removeListener(playerListener)

        notiManager?.setPlayer(null)
        notiManager = null

        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //### 앱 날림이면 재생/노티/서비스 전부 종료
        player.stop()
        player.clearMediaItems()

        stopForeground(true)
        isInForeground = false
        nm.cancel(NOTI_ID)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                runCatching { ensureForegroundStartedFast() }
                notiManager?.invalidate()
            }

            ACTION_INVALIDATE -> {
                notiManager?.invalidate()
            }

            ACTION_STOP -> {
                //## 플레이어 정리 + (옵션) 노티 제거
                val remove = intent.getBooleanExtra(EXTRA_REMOVE_NOTIFICATION, true)

                player.pause()
                player.seekTo(0L)
                player.clearMediaItems()

                if (remove) {
                    stopForeground(true)
                    isInForeground = false
                    nm.cancel(NOTI_ID)
                } else {
                    if (isInForeground) {
                        stopForeground(false)
                        isInForeground = false
                    }
                    notiManager?.invalidate()
                }

                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                notiManager?.invalidate()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * 실질적으로 재생 중인지 판정
     * ---------------------------------------------------------------------------------------------
     *
     * ENDED면 false
     * playWhenReady 기준으로 foreground 유지 여부를 결정
     */
    private fun isEffectivelyPlaying(): Boolean {
        if (player.playbackState == Player.STATE_ENDED) return false
        return player.playWhenReady
    }

    /**
     * FGS 타임아웃 방지용: 임시 노티로 빠르게 startForeground 보장
     * ---------------------------------------------------------------------------------------------
     *
     * ACTION_START(사용자 액션)에서만 호출하는 전제
     * startForeground 실패 시 앱 크래시 방지: notify로 대체
     * 일시정지 상태면 즉시 stopForeground(false)로 detach
     */
    private fun ensureForegroundStartedFast() {
        if (isInForeground) return

        val placeholder = buildPlaceholderNotification(isOngoing = isEffectivelyPlaying())

        runCatching {
            startForeground(NOTI_ID, placeholder)
            isInForeground = true
        }.onFailure {
            isInForeground = false
            nm.notify(NOTI_ID, placeholder)
            return
        }

        if (!isEffectivelyPlaying()) {
            stopForeground(false)
            isInForeground = false
            nm.notify(NOTI_ID, buildPlaceholderNotification(isOngoing = false))
        }
    }

    //### Player 이벤트 수신 → 노티 invalidate (상태/메타/트랙 변경 반영)
    private val playerListener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            //## 트랙 전환 시: 앨범아트 콜백 세대 증가(이전 요청 무시)
            artworkGeneration.incrementAndGet()
            notiManager?.invalidate()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            notiManager?.invalidate()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            notiManager?.invalidate()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            notiManager?.invalidate()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            notiManager?.invalidate()
        }
    }

    //### 노티 게시/취소 이벤트에서 FGS attach/detach 정책 처리
    private val notificationListener = object : PlayerNotificationManager.NotificationListener {

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            val shouldForeground = isEffectivelyPlaying()

            if (shouldForeground) {
                //### 재생 중: 포그라운드 유지 + 스와이프 불가
                if (!isInForeground) {
                    runCatching {
                        startForeground(notificationId, notification)
                        isInForeground = true
                    }.onFailure {
                        isInForeground = false
                        nm.notify(notificationId, notification)
                    }
                } else {
                    nm.notify(notificationId, notification)
                }
            } else {
                //### 일시정지: 포그라운드 해제(노티 유지) + 스와이프 가능
                if (isInForeground) {
                    stopForeground(false)
                    isInForeground = false
                }
                nm.notify(notificationId, notification)
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            isInForeground = false

            //## 사용자가 직접 스와이프해서 없앴다면 서비스도 종료
            if (dismissedByUser) {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    //### 노티 제목/본문/앨범아트/클릭 인텐트 제공
    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            return player.mediaMetadata.title?.toString().orEmpty().ifBlank { "Music" }
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val trackId = player.currentMediaItem?.mediaId?.toLongOrNull()

            val launchIntent =
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    action = ACTION_OPEN_DETAIL_FROM_NOTIFICATION
                    if (trackId != null) putExtra(EXTRA_TRACK_ID, trackId)
                } ?: return null

            val reqCode = (trackId ?: 0L).hashCode()

            return PendingIntent.getActivity(
                this@MusicPlaybackService,
                reqCode,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return player.mediaMetadata.artist?.toString().orEmpty().ifBlank { null }
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val uri = player.mediaMetadata.artworkUri ?: return null
            val genAtRequest = artworkGeneration.get()

            val req = ImageRequest.Builder(this@MusicPlaybackService)
                .data(uri)
                .allowHardware(false)
                .target(object : Target {
                    override fun onSuccess(result: android.graphics.drawable.Drawable) {
                        //## 트랙이 바뀐 뒤 도착한 이전 결과면 무시
                        if (genAtRequest != artworkGeneration.get()) return
                        callback.onBitmap(result.toBitmap())
                    }

                    override fun onError(error: android.graphics.drawable.Drawable?) {
                        if (genAtRequest != artworkGeneration.get()) return
                    }
                })
                .build()

            imageLoader.enqueue(req)
            return null
        }
    }

    /**
     * startForeground "즉시 호출"을 위한 임시 노티 생성
     * ---------------------------------------------------------------------------------------------
     *
     * PlayerNotificationManager가 아직 노티를 만들기 전이라도, 타임아웃을 막기 위해 사용
     * isOngoing 값으로 스와이프 가능/불가를 최소한으로 맞춤
     */
    private fun buildPlaceholderNotification(isOngoing: Boolean): Notification {
        val title = player.mediaMetadata.title?.toString().orEmpty().ifBlank { "Music" }
        val artist = player.mediaMetadata.artist?.toString().orEmpty()

        val trackId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = ACTION_OPEN_DETAIL_FROM_NOTIFICATION
                if (trackId != null) putExtra(EXTRA_TRACK_ID, trackId)
            }

        val contentPI = launchIntent?.let {
            val reqCode = (trackId ?: 0L).hashCode()
            PendingIntent.getActivity(
                this,
                reqCode,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentPI)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setOngoing(isOngoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Android 8.0+ NotificationChannel 생성
     * ---------------------------------------------------------------------------------------------
     *
     * IMPORTANCE_LOW로 재생 노티를 조용하게 표시
     */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}

