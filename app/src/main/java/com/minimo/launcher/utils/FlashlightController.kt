package com.minimo.launcher.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
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

    init {
        if (torchCameraId != null) {
            cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchCameraId) _isOn.value = enabled
                }

                override fun onTorchModeUnavailable(cameraId: String) {
                    // The camera is used by another app: the torch is off for us
                    if (cameraId == torchCameraId) _isOn.value = false
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    /** @return false when the torch could not be switched (no flash, camera busy). */
    fun toggle(): Boolean {
        val id = torchCameraId ?: return false
        return try {
            cameraManager?.setTorchMode(id, !_isOn.value)
            true
        } catch (exception: Exception) {
            Timber.e(exception)
            false
        }
    }
}
