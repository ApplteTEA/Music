package com.test.presentation.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.test.presentation.util.findActivity

enum class AudioPermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

fun hasAudioPermission(context: Context): Boolean {
    val permission = audioPermission()
    return ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/**
 * 앱/화면 진입 시 자동 권한 팝업을 띄우고 현재 권한 상태를 AudioPermissionStatus로 반환.
 * -------------------------------------------------------------------------------------------------
 *
 * GRANTED: 허용됨
 * DENIED: 거절(다시 묻기 가능 상태)
 * PERMANENTLY_DENIED: “다시 묻지 않음” 포함 거절 상태 (설정 이동 유도)
 *
 * ListScreen에서 status에 따라 UI를 분기하면 됨.
 */
@Composable
fun rememberAudioPermissionState(
    requestOnEnter: Boolean = true
): AudioPermissionController {
    val context = LocalContext.current
    val activity = context.findActivity()
    val permission = remember { audioPermission() }

    var status by rememberSaveable {
        mutableStateOf(
            if (hasAudioPermission(context)) AudioPermissionStatus.GRANTED
            else AudioPermissionStatus.DENIED
        )
    }

    fun refreshStatusAfterDenied() {
        val permanentlyDenied =
            activity != null && !activity.shouldShowRequestPermissionRationale(permission)
        status = if (permanentlyDenied) AudioPermissionStatus.PERMANENTLY_DENIED
        else AudioPermissionStatus.DENIED
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) AudioPermissionStatus.GRANTED else AudioPermissionStatus.DENIED
        if (!granted) refreshStatusAfterDenied()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, requestOnEnter, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && requestOnEnter) {
                if (hasAudioPermission(context)) {
                    status = AudioPermissionStatus.GRANTED
                } else if (!requestedOnce) {
                    requestedOnce = true
                    launcher.launch(permission)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission(context)) status = AudioPermissionStatus.GRANTED
    }

    return remember {
        AudioPermissionController(
            getStatus = { status },
            request = { launcher.launch(permission) },
            openSettings = { openAppSettings(context) }
        )
    }
}

class AudioPermissionController internal constructor(
    private val getStatus: () -> AudioPermissionStatus,
    val request: () -> Unit,
    val openSettings: () -> Unit
) {
    val status: AudioPermissionStatus get() = getStatus()
}
