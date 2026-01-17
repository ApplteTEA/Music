package com.test.music.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.test.music.R
import com.test.playback.controller.PlaybackController
import com.test.playback.service.MusicPlaybackService
import com.test.presentation.navigation.AppNavHost
import com.test.presentation.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playbackController: PlaybackController

    private val pendingIntents = mutableStateListOf<Intent>()

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            var lastBackAt by remember { mutableLongStateOf(0L) }

            BackHandler {
                val route = navController.currentBackStackEntry?.destination?.route

                if (route == Routes.LIST) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackAt <= 2_000L) {
                        runCatching { playbackController.stopAndReset() }
                        finish()
                    } else {
                        lastBackAt = now
                        Toast.makeText(this, "뒤로가기 버튼을 한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    navController.popBackStack()
                }
            }

            LaunchedEffect(Unit) {
                handleIntentNavigate(intent, navController)
            }

            LaunchedEffect(pendingIntents.size) {
                if (pendingIntents.isEmpty()) return@LaunchedEffect
                val copy = pendingIntents.toList()
                pendingIntents.clear()
                copy.forEach { handleIntentNavigate(it, navController) }
            }

            Scaffold(
                contentWindowInsets = WindowInsets.systemBars
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AppNavHost(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntents.add(intent)
    }

    /**
     * Notification 클릭 후 DetailScreen 이동 처리
     * ---------------------------------------------------------------------------------------------
     *
     */
    @UnstableApi
    private fun handleIntentNavigate(intent: Intent, navController: NavHostController) {
        if (intent.action != MusicPlaybackService.ACTION_OPEN_DETAIL_FROM_NOTIFICATION) return

        val trackId = intent.getLongExtra(MusicPlaybackService.EXTRA_TRACK_ID, -1L)
        if (trackId <= 0L) return

        navController.navigate(Routes.detail(trackId)) {
            launchSingleTop = true
            restoreState = false
            popUpTo(Routes.LIST) { inclusive = false }
        }

        intent.action = null
        intent.removeExtra(MusicPlaybackService.EXTRA_TRACK_ID)
    }
}
