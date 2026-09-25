package com.minimo.launcher.ui.home

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.minimo.launcher.R
import com.minimo.launcher.utils.HomeButton
import com.minimo.launcher.utils.HomeButtonType
import com.minimo.launcher.utils.hasLockScreenPermission
import com.minimo.launcher.utils.lockScreen
import com.minimo.launcher.utils.openDefaultClockApp
import com.minimo.launcher.utils.requestLockScreenPermission
import com.minimo.launcher.utils.showNotificationDrawer
import timber.log.Timber

/** Runs the action of a home screen quick button. */
fun Context.performHomeButtonAction(
    button: HomeButton,
    viewModel: HomeViewModel,
    onOpenAppDrawer: () -> Unit,
    onSettingsClick: () -> Unit
) {
    when (button.type) {
        HomeButtonType.FLASHLIGHT -> viewModel.onFlashlightClick()
        HomeButtonType.CAMERA -> startSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        HomeButtonType.PHONE -> startSafely(Intent(Intent.ACTION_DIAL))
        HomeButtonType.MESSAGES -> startSafely(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
        )

        HomeButtonType.BROWSER -> startSafely(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        )

        HomeButtonType.CALCULATOR -> startSafely(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
        )

        HomeButtonType.ALARM -> openDefaultClockApp()
        HomeButtonType.WIFI -> startSafely(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
            else Intent(Settings.ACTION_WIFI_SETTINGS)
        )

        HomeButtonType.BLUETOOTH -> startSafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        HomeButtonType.VOLUME -> showVolumePanel()
        HomeButtonType.MEDIA_PLAY_PAUSE -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        HomeButtonType.MEDIA_NEXT -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        HomeButtonType.LOCK_SCREEN -> {
            if (hasLockScreenPermission()) lockScreen() else requestLockScreenPermission()
        }

        HomeButtonType.NOTIFICATIONS -> showNotificationDrawer()
        HomeButtonType.SEARCH -> onOpenAppDrawer()
        HomeButtonType.SETTINGS -> onSettingsClick()
        HomeButtonType.APP -> {
            if (!viewModel.onPreferenceAppLaunchRequest(button.app)) {
                Toast.makeText(this, R.string.home_button_app_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (exception: Exception) {
        Timber.e(exception)
        Toast.makeText(this, R.string.home_button_failed, Toast.LENGTH_SHORT).show()
    }
}

/** Shows the system volume slider for media. */
private fun Context.showVolumePanel() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        AudioManager.ADJUST_SAME,
        AudioManager.FLAG_SHOW_UI
    )
}

/** Play/pause or next track in whatever player is active (no permission needed). */
private fun Context.sendMediaKey(keyCode: Int) {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}
