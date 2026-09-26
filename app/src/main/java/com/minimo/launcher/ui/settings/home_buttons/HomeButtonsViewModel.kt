package com.minimo.launcher.ui.settings.home_buttons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimo.launcher.data.AppInfoDao
import com.minimo.launcher.data.HomeButtonsSettings
import com.minimo.launcher.data.PreferenceHelper
import com.minimo.launcher.ui.entities.AppPreferenceTarget
import com.minimo.launcher.ui.entities.toAppPreferenceTarget
import com.minimo.launcher.utils.HomeButton
import com.minimo.launcher.utils.HomeButtonSize
import com.minimo.launcher.utils.HomeButtonStyle
import com.minimo.launcher.utils.HomeButtonType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeButtonsViewModel @Inject constructor(
    private val preferenceHelper: PreferenceHelper,
    private val appInfoDao: AppInfoDao
) : ViewModel() {

    val settings: StateFlow<HomeButtonsSettings> = preferenceHelper.getHomeButtonsSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeButtonsSettings())

    /** Apps that can go into a folder (hidden apps are left out). */
    val folderChoices: StateFlow<List<FolderAppChoice>> = appInfoDao.getAllAppsFlow()
        .map { entities ->
            entities.filterNot { it.isHidden }.map { entity ->
                FolderAppChoice(
                    preference = AppPreferenceTarget(
                        itemType = entity.itemType,
                        packageName = entity.packageName,
                        targetId = entity.targetId,
                        userHandle = entity.userHandle
                    ).preferenceValue,
                    name = entity.alternateAppName.ifEmpty { entity.appName }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addFolder(name: String, apps: List<String>) {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons ->
                val index = buttons.size
                val x = 0.1f + (index % 5) * 0.2f
                val y = (0.92f - (index / 5) * 0.1f).coerceAtLeast(0.1f)
                buttons + HomeButton(
                    type = HomeButtonType.FOLDER, x = x, y = y, name = name, apps = apps
                )
            }
        }
    }

    fun updateFolder(folder: HomeButton, name: String, apps: List<String>) {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons ->
                buttons.map { if (it.id == folder.id) it.copy(name = name, apps = apps) else it }
            }
        }
    }

    fun addButton(type: HomeButtonType, app: String = "") {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons ->
                // New buttons line up along the bottom; the user drags them where needed
                val index = buttons.size
                val x = 0.1f + (index % 5) * 0.2f
                val y = (0.92f - (index / 5) * 0.1f).coerceAtLeast(0.1f)
                buttons + HomeButton(type = type, app = app, x = x, y = y)
            }
        }
    }

    fun removeButton(button: HomeButton) {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons -> buttons.filterNot { it.id == button.id } }
        }
    }

    fun setSize(size: HomeButtonSize) {
        viewModelScope.launch { preferenceHelper.setHomeButtonSize(size) }
    }

    fun toggleFlashlightAutoOff() {
        viewModelScope.launch {
            preferenceHelper.setFlashlightAutoOff(!settings.value.flashlightAutoOff)
        }
    }

    fun setStyle(style: HomeButtonStyle) {
        viewModelScope.launch { preferenceHelper.setHomeButtonStyle(style) }
    }

    /** Display name of the app behind an app button (empty if it is not installed any more). */
    suspend fun appName(preference: String): String {
        val target = preference.toAppPreferenceTarget() ?: return ""
        return appInfoDao.getAllApps()
            .firstOrNull { entity ->
                entity.packageName == target.packageName && entity.targetId == target.targetId
            }
            ?.let { it.alternateAppName.ifEmpty { it.appName } }
            .orEmpty()
    }
}
