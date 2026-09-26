package com.minimo.launcher.ui.hidden_apps

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.minimo.launcher.R
import com.minimo.launcher.data.PreferenceHelper
import com.minimo.launcher.utils.AppLock
import com.minimo.launcher.utils.findFragmentActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val preferenceHelper: PreferenceHelper,
    val appLock: AppLock
) : ViewModel() {
    /** null while loading */
    val protectHiddenApps: StateFlow<Boolean?> = preferenceHelper.getHomePreferencesFlow()
        .map<_, Boolean?> { it.protectHiddenApps }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setProtection(enabled: Boolean) {
        viewModelScope.launch { preferenceHelper.setProtectHiddenApps(enabled) }
    }
}

/**
 * Shows [content] only after the phone lock check when hidden apps are protected;
 * cancelling the check leaves the screen via [onCancel].
 */
@Composable
fun HiddenAppsGate(
    onCancel: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val protect by viewModel.protectHiddenApps.collectAsStateWithLifecycle()
    val unlocked by viewModel.appLock.unlocked.collectAsStateWithLifecycle()

    when {
        protect == null -> Unit
        protect == false || unlocked -> content()
        else -> {
            // Keep the list invisible behind the system prompt
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
            LaunchedEffect(Unit) {
                val activity = context.findFragmentActivity()
                if (activity == null) {
                    onCancel()
                    return@LaunchedEffect
                }
                viewModel.appLock.authenticate(
                    activity,
                    title = context.getString(R.string.unlock_hidden_apps),
                    subtitle = context.getString(R.string.unlock_hidden_apps_subtitle)
                ) { success -> if (!success) onCancel() }
            }
        }
    }
}

/** Switch handler: turning protection on first checks that the phone has a screen lock. */
fun AppLockViewModel.toggleProtection(context: android.content.Context, currentlyOn: Boolean) {
    if (currentlyOn) {
        setProtection(false)
        return
    }
    if (!appLock.canAuthenticate()) {
        Toast.makeText(context, R.string.protect_needs_screen_lock, Toast.LENGTH_LONG).show()
        return
    }
    val activity = context.findFragmentActivity() ?: return
    appLock.authenticate(
        activity,
        title = context.getString(R.string.protect_hidden_apps),
        subtitle = context.getString(R.string.unlock_hidden_apps_subtitle)
    ) { success -> if (success) setProtection(true) }
}
