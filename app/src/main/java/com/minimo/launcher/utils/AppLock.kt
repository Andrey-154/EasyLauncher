package com.minimo.launcher.utils

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Protects hidden apps with the phone's own lock: fingerprint/face or the screen PIN/pattern.
 * Once unlocked, it stays unlocked until the launcher goes to the background or the screen
 * turns off.
 */
@Singleton
class AppLock @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    init {
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = lock()
            },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun lock() {
        _unlocked.value = false
    }

    /** False when the phone has no screen lock at all (nothing to check against). */
    fun canAuthenticate(): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isDeviceSecure
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit
    ) {
        if (_unlocked.value) {
            onResult(true)
            return
        }
        try {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        _unlocked.value = true
                        onResult(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onResult(false)
                    }
                    // onAuthenticationFailed (a wrong finger) keeps the prompt open for another try
                }
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                    .build()
            )
        } catch (exception: Exception) {
            Timber.e(exception)
            onResult(false)
        }
    }
}

/** The activity behind a Compose context (it may be wrapped, e.g. by a theme). */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
