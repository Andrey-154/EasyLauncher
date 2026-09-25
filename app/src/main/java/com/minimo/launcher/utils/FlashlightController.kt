package com.minimo.launcher.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the camera flash on/off as a torch (no camera permission needed) and tracks its real
 * state, so the home screen stays in sync when the torch is toggled from quick settings.
 */
@Singleton
class FlashlightController @Inject constructor(
    @ApplicationContext context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    /** Back camera with a flash, or any camera with a flash, or null when there is none. */
    private val torchCameraId: String? = try {
        val withFlash = cameraManager?.cameraIdList.orEmpty().filter { id ->
            cameraManager?.getCameraCharacteristics(id)
                ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        withFlash.firstOrNull { id ->
            cameraManager?.getCameraCharacteristics(id)
                ?.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    } catch (exception: Exception) {
        Timber.e(exception)
        null
    }

    val isAvailable: Boolean get() = torchCameraId != null

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    /** Only a torch turned on from the launcher is turned off with the screen. */
    private var turnedOnByLauncher = false

    init {
        if (torchCameraId != null) {
            // Turn our torch off when the screen goes off, so it does not drain the battery in a pocket
            ContextCompat.registerReceiver(
                context,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (_isOn.value && turnedOnByLauncher) setTorch(false)
                    }
                },
                IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId != torchCameraId) return
                    _isOn.value = enabled
                    if (!enabled) turnedOnByLauncher = false
                }

                override fun onTorchModeUnavailable(cameraId: String) {
                    // The camera is used by another app: the torch is off for us
                    if (cameraId != torchCameraId) return
                    _isOn.value = false
                    turnedOnByLauncher = false
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    /** @return false when the torch could not be switched (no flash, camera busy). */
    fun toggle(): Boolean {
        val enable = !_isOn.value
        val switched = setTorch(enable)
        if (switched) turnedOnByLauncher = enable
        return switched
    }

    private fun setTorch(enabled: Boolean): Boolean {
        val id = torchCameraId ?: return false
        return try {
            cameraManager?.setTorchMode(id, enabled)
            // Update right away: the system callback arrives later, and a quick second tap
            // must see the new state (the callback then confirms or corrects it)
            _isOn.value = enabled
            true
        } catch (exception: Exception) {
            Timber.e(exception)
            false
        }
    }
}
