package com.minimo.launcher.ui.home

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimo.launcher.R
import com.minimo.launcher.data.AppInfoDao
import com.minimo.launcher.data.PreferenceHelper
import com.minimo.launcher.data.entities.AppItemType
import com.minimo.launcher.data.usecase.UpdateAllAppsUseCase
import com.minimo.launcher.data.usecase.UpdateAllShortcutsUseCase
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.ui.entities.toAppPreferenceTarget
import com.minimo.launcher.utils.AppIconRepository
import com.minimo.launcher.utils.AppUtils
import com.minimo.launcher.utils.Constants
import com.minimo.launcher.utils.HomeAppsAlignmentHorizontal
import com.minimo.launcher.utils.HomeAppsAlignmentVertical
import com.minimo.launcher.utils.HomeClockAlignment
import com.minimo.launcher.utils.MinimoSettingsPosition
import com.minimo.launcher.utils.NotificationDotsNotifier
import com.minimo.launcher.utils.ScreenTimeHelper
import com.minimo.launcher.utils.SearchMode
import com.minimo.launcher.utils.ShortcutsUtils
import com.minimo.launcher.utils.AppLock
import com.minimo.launcher.utils.CarouselSound
import com.minimo.launcher.utils.CarouselHapticPlayer
import com.minimo.launcher.utils.CarouselSoundPlayer
import com.minimo.launcher.utils.FlashlightController
import com.minimo.launcher.utils.HomeButton
import com.minimo.launcher.utils.HomeButtonType
import com.minimo.launcher.utils.LaunchStatsRepository
import com.minimo.launcher.utils.StringUtils
import com.minimo.launcher.utils.TimeLimitRepository
import com.minimo.launcher.utils.WeatherForecast
import com.minimo.launcher.utils.WeatherRepository
import com.minimo.launcher.utils.formatTemperature
import com.minimo.launcher.utils.isAppUsagePermissionGranted
import com.minimo.launcher.utils.launchApp
import com.minimo.launcher.utils.startShortcut
import com.minimo.launcher.utils.updateNotificationDots
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val updateAllAppsUseCase: UpdateAllAppsUseCase,
    private val appInfoDao: AppInfoDao,
    private val appUtils: AppUtils,
    private val preferenceHelper: PreferenceHelper,
    private val notificationDotsNotifier: NotificationDotsNotifier,
    @ApplicationContext
    private val applicationContext: Context,
    private val screenTimeHelper: ScreenTimeHelper,
    private val updateAllShortcutsUseCase: UpdateAllShortcutsUseCase,
    private val shortcutsUtils: ShortcutsUtils,
    private val appIconRepository: AppIconRepository,
    private val weatherRepository: WeatherRepository,
    private val launchStats: LaunchStatsRepository,
    private val flashlightController: FlashlightController,
    private val timeLimitRepository: TimeLimitRepository,
    private val appLock: AppLock,
    private val carouselSoundPlayer: CarouselSoundPlayer,
    private val carouselHapticPlayer: CarouselHapticPlayer
) : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state
    val iconCacheRevision = appIconRepository.cacheRevision
    val flashlightOn: StateFlow<Boolean> = flashlightController.isOn

    /** False on devices without a flash (e.g. the setting came from a backup of another phone). */
    val flashlightAvailable: Boolean = flashlightController.isAvailable

    private var lastScreenTimeUpdateTime = 0L
    private var lastWeatherUpdateTime = 0L
    private var lastAppScreenTimeUpdateTime = 0L

    // Forecast cache: reopening the dialog within 30 minutes shows it instantly, without network
    private var cachedForecast: WeatherForecast? = null
    private var cachedForecastTime = 0L
    private var cachedForecastLocation: Pair<Double, Double>? = null

    /** Mirrors the preference; read by [getAppsWithSearch] which runs inside state updates. */
    private var sortAppsByUsage = false

    init {
        viewModelScope.launch {
            updateAllAppsUseCase.invoke()
        }

        viewModelScope.launch {
            updateAllShortcutsUseCase.invoke()
        }

        viewModelScope.launch {
            appInfoDao.getAllAppsFlow()
                .collect { appInfoList ->
                    val dbApps = appUtils.mapToAppInfo(
                        entities = appInfoList,
                        notificationDots = notificationDotsNotifier.getNotificationDots()
                    )

                    _state.update { state ->
                        val allApps = getCombinedAllApps(
                            dbApps = dbApps,
                            hideAppDrawerSearch = state.hideAppDrawerSearch,
                            minimoSettingsPosition = state.minimoSettingsPosition
                        )

                        state.copy(
                            allApps = allApps,
                            filteredAllApps = getAppsWithSearch(
                                searchText = state.searchText,
                                apps = allApps,
                                includeHiddenApps = state.showHiddenAppsInSearch,
                                ignoreSpecialCharacters = state.ignoreSpecialCharacters,
                                searchMode = state.searchMode
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            preferenceHelper.getHomeButtonsSettingsFlow().collect { settings ->
                _state.update {
                    it.copy(
                        homeButtons = settings.buttons,
                        homeButtonSize = settings.size,
                        homeButtonStyle = settings.style
                    )
                }
            }
        }

        viewModelScope.launch {
            timeLimitRepository.limits.collect { limits ->
                _state.update { it.copy(timeLimits = limits) }
                refreshAppScreenTime(force = true)
            }
        }

        viewModelScope.launch {
            launchStats.counts.collect {
                if (!sortAppsByUsage) return@collect
                _state.update { state ->
                    state.copy(
                        filteredAllApps = getAppsWithSearch(
                            searchText = state.searchText,
                            apps = state.allApps,
                            includeHiddenApps = state.showHiddenAppsInSearch,
                            ignoreSpecialCharacters = state.ignoreSpecialCharacters,
                            searchMode = state.searchMode
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            appInfoDao.getFavouriteAppsFlow()
                .collect { appInfoList ->
                    _state.update {
                        it.copy(
                            initialLoaded = true,
                            favouriteApps = appUtils.mapToAppInfo(
                                entities = appInfoList,
                                notificationDots = notificationDotsNotifier.getNotificationDots()
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            notificationDotsNotifier.notificationDots.collect { notificationDotSet ->
                val allApps = _state.value.allApps.updateNotificationDots(notificationDotSet)
                val favouriteApps =
                    _state.value.favouriteApps.updateNotificationDots(notificationDotSet)
                _state.update {
                    it.copy(
                        allApps = allApps,
                        filteredAllApps = getAppsWithSearch(
                            searchText = it.searchText,
                            apps = allApps,
                            includeHiddenApps = it.showHiddenAppsInSearch,
                            ignoreSpecialCharacters = it.ignoreSpecialCharacters,
                            searchMode = it.searchMode
                        ),
                        favouriteApps = favouriteApps
                    )
                }
            }
        }

        viewModelScope.launch {
            preferenceHelper.getHomePreferencesFlow()
                .distinctUntilChanged()
                .collect { prefs ->
                    if (!prefs.showAppIconInHome &&
                        !prefs.showAppIconInDrawer &&
                        (_state.value.showAppIconInHome || _state.value.showAppIconInDrawer)
                    ) {
                        appIconRepository.clear()
                    }

                    _state.update { state ->
                        val homeAppsArrangementHorizontal =
                            when (prefs.homeAppsAlignmentHorizontal) {
                                HomeAppsAlignmentHorizontal.Start -> Arrangement.Start
                                HomeAppsAlignmentHorizontal.Center -> Arrangement.Center
                                HomeAppsAlignmentHorizontal.End -> Arrangement.End
                            }

                        val drawerAppsArrangementHorizontal =
                            when (prefs.drawerAppsAlignmentHorizontal) {
                                HomeAppsAlignmentHorizontal.Start -> Arrangement.Start
                                HomeAppsAlignmentHorizontal.Center -> Arrangement.Center
                                HomeAppsAlignmentHorizontal.End -> Arrangement.End
                            }

                        val homeAppsArrangementVertical = when (prefs.homeAppsAlignmentVertical) {
                            HomeAppsAlignmentVertical.Top -> Arrangement.Top
                            HomeAppsAlignmentVertical.Center -> Arrangement.Center
                            HomeAppsAlignmentVertical.Bottom -> Arrangement.Bottom
                        }

                        val homeClockAlignment = when (prefs.homeClockAlignment) {
                            HomeClockAlignment.Start -> Alignment.Start
                            HomeClockAlignment.Center -> Alignment.CenterHorizontally
                            HomeClockAlignment.End -> Alignment.End
                        }

                        var newAllApps = state.allApps
                        var newFilteredApps = state.filteredAllApps
                        var clearSearchText = state.searchText

                        if (state.hideAppDrawerSearch != prefs.hideAppDrawerSearch || state.minimoSettingsPosition != prefs.minimoSettingsPosition) {
                            val dbApps =
                                state.allApps.filterNot { it.packageName == Constants.MINIMO_SETTINGS_PACKAGE }
                            newAllApps = getCombinedAllApps(
                                dbApps = dbApps,
                                hideAppDrawerSearch = prefs.hideAppDrawerSearch,
                                minimoSettingsPosition = prefs.minimoSettingsPosition
                            )
                            if (prefs.hideAppDrawerSearch) {
                                clearSearchText = ""
                            }
                            newFilteredApps = getAppsWithSearch(
                                searchText = clearSearchText,
                                apps = newAllApps,
                                includeHiddenApps = prefs.showHiddenAppsInSearch,
                                ignoreSpecialCharacters = prefs.ignoreSpecialCharacters,
                                searchMode = prefs.searchMode
                            )
                        } else if (
                            state.showHiddenAppsInSearch != prefs.showHiddenAppsInSearch ||
                            state.ignoreSpecialCharacters != prefs.ignoreSpecialCharacters ||
                            state.searchMode != prefs.searchMode
                        ) {
                            newFilteredApps = getAppsWithSearch(
                                searchText = clearSearchText,
                                apps = newAllApps,
                                includeHiddenApps = prefs.showHiddenAppsInSearch,
                                ignoreSpecialCharacters = prefs.ignoreSpecialCharacters,
                                searchMode = prefs.searchMode
                            )
                        }

                        flashlightController.autoOffWithScreen = prefs.flashlightAutoOff
                        if (prefs.carouselSound != CarouselSound.Off) carouselSoundPlayer.prepare()

                        // The old "flashlight next to the clock" option became a movable button
                        if (prefs.showFlashlight) migrateFlashlightToButton()

                        if (prefs.sortAppsByUsage != sortAppsByUsage) {
                            sortAppsByUsage = prefs.sortAppsByUsage
                            newFilteredApps = getAppsWithSearch(
                                searchText = clearSearchText,
                                apps = newAllApps,
                                includeHiddenApps = prefs.showHiddenAppsInSearch,
                                ignoreSpecialCharacters = prefs.ignoreSpecialCharacters,
                                searchMode = prefs.searchMode
                            )
                        }

                        // Reload weather when it gets enabled or the city changes
                        val weatherLocationChanged =
                            prefs.weatherLatitude != state.weatherLatitude ||
                                    prefs.weatherLongitude != state.weatherLongitude
                        val newWeatherText = when {
                            !prefs.showWeather || weatherLocationChanged -> ""
                            else -> state.weatherText
                        }
                        if (prefs.showWeather && (!state.showWeather || weatherLocationChanged)) {
                            loadWeather(prefs.weatherLatitude, prefs.weatherLongitude)
                        }

                        // Refresh screen time when the preference flag is enabled
                        if (prefs.showScreenTimeWidget && !state.showScreenTimeWidget) {
                            refreshScreenTime()
                        }

                        state.copy(
                            searchPreferencesLoaded = true,
                            appsArrangementHorizontal = homeAppsArrangementHorizontal,
                            drawerAppsArrangementHorizontal = drawerAppsArrangementHorizontal,
                            appsArrangementVertical = homeAppsArrangementVertical,
                            homeClockAlignment = homeClockAlignment,
                            showHomeClock = prefs.showHomeClock,
                            homeTextSize = prefs.homeTextSize,
                            autoOpenKeyboardAllApps = prefs.autoOpenKeyboardAllApps,
                            homeClockMode = prefs.homeClockMode,
                            doubleTapToLock = prefs.doubleTapToLock,
                            twentyFourHourFormat = prefs.twentyFourHourFormat,
                            showBatteryLevel = prefs.showBatteryLevel,
                            showHiddenAppsInSearch = prefs.showHiddenAppsInSearch,
                            drawerSearchBarAtBottom = prefs.drawerSearchBarAtBottom,
                            showAppIconInHome = prefs.showAppIconInHome,
                            showAppIconInDrawer = prefs.showAppIconInDrawer,
                            homeAppIconAlignment = prefs.homeAppIconAlignment,
                            drawerAppIconAlignment = prefs.drawerAppIconAlignment,
                            appIconSizePercent = prefs.appIconSizePercent,
                            applyHomeAppSizeToAllApps = prefs.applyHomeAppSizeToAllApps,
                            autoOpenApp = prefs.autoOpenApp,
                            homeAppVerticalPadding = prefs.homeAppVerticalPadding,
                            ignoreSpecialCharacters = prefs.ignoreSpecialCharacters,
                            searchMode = prefs.searchMode,
                            searchBarBackground = prefs.searchBarBackground,
                            searchBarBorderPercent = prefs.searchBarBorderPercent,
                            hideAppDrawerSearch = prefs.hideAppDrawerSearch,
                            hideSettingsIcon = prefs.hideSettingsIcon,
                            minimoSettingsPosition = prefs.minimoSettingsPosition,
                            enableWallpaper = prefs.enableWallpaper,
                            enableWallpaperOnDrawer = prefs.enableWallpaperOnDrawer,
                            showScreenTimeWidget = prefs.showScreenTimeWidget,
                            lightTextOnWallpaper = prefs.lightTextOnWallpaper,
                            clockAppPreference = prefs.clockAppPreference,
                            batteryAppPreference = prefs.batteryAppPreference,
                            calendarAppPreference = prefs.calendarAppPreference,
                            screenTimeAppPreference = prefs.screenTimeAppPreference,
                            swipeLeftAppPreference = prefs.swipeLeftAppPreference,
                            swipeRightAppPreference = prefs.swipeRightAppPreference,
                            keyboardOpenDelay = prefs.keyboardOpenDelay,
                            enableFastScroller = prefs.enableFastScroller,
                            fastScrollerAlignment = prefs.fastScrollerAlignment,
                            backOpensAppDrawer = prefs.backOpensAppDrawer,
                            compactAppTouchArea = prefs.compactAppTouchArea,
                            keyboardDoneOpensFirstApp = prefs.keyboardDoneOpensFirstApp,
                            showWeather = prefs.showWeather,
                            weatherCity = prefs.weatherCity,
                            weatherLatitude = prefs.weatherLatitude,
                            weatherLongitude = prefs.weatherLongitude,
                            weatherText = newWeatherText,
                            homeClockStyle = prefs.homeClockStyle,
                            showHomeNote = prefs.showHomeNote,
                            homeNote = prefs.homeNote,
                            sortAppsByUsage = prefs.sortAppsByUsage,
                            showFlashlight = prefs.showFlashlight,
                            limitColorTimeOnly = prefs.limitColorTimeOnly,
                            limitHomeApps = prefs.limitHomeApps,
                            maxHomeApps = prefs.maxHomeApps,
                            protectHiddenApps = prefs.protectHiddenApps,
                            carouselVibration = prefs.carouselVibration,
                            carouselSound = prefs.carouselSound,
                            carouselSoundVolume = prefs.carouselSoundVolume,
                            limitWarningColor = prefs.limitWarningColor
                                ?: TimeLimitRepository.DEFAULT_WARNING_COLOR,
                            limitExceededColor = prefs.limitExceededColor
                                ?: TimeLimitRepository.DEFAULT_EXCEEDED_COLOR,
                            showAppScreenTime = prefs.showAppScreenTime,
                            appScreenTime = if (prefs.showAppScreenTime) state.appScreenTime else emptyMap(),
                            allApps = newAllApps,
                            filteredAllApps = newFilteredApps,
                            searchText = clearSearchText
                        )
                    }
                }
        }
    }

    suspend fun loadAppIcon(app: AppInfo, sizePx: Int) = appIconRepository.loadIcon(
        packageName = app.packageName,
        itemType = app.itemType,
        targetId = app.targetId,
        userHandle = app.userHandle,
        sizePx = sizePx
    )

    private fun getCombinedAllApps(
        dbApps: List<AppInfo>,
        hideAppDrawerSearch: Boolean,
        minimoSettingsPosition: MinimoSettingsPosition
    ): List<AppInfo> {
        return if (hideAppDrawerSearch) {
            val settingsAppInfo = AppInfo(
                packageName = Constants.MINIMO_SETTINGS_PACKAGE,
                itemType = AppItemType.APP,
                targetId = "",
                userHandle = 0,
                appName = applicationContext.getString(R.string.minimo_settings),
                alternateAppName = "",
                isFavourite = false,
                isHidden = false,
                isWorkProfile = false,
                showNotificationDot = false,
                orderIndex = 0
            )
            when (minimoSettingsPosition) {
                MinimoSettingsPosition.Top -> listOf(settingsAppInfo) + dbApps
                MinimoSettingsPosition.Bottom -> dbApps + listOf(settingsAppInfo)
                MinimoSettingsPosition.Auto -> (dbApps + settingsAppInfo).sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
        } else {
            dbApps
        }
    }

    fun onAppDrawerClosed() {
        if (_state.value.searchText.isNotBlank()) {
            onSearchTextChange("")
        }
    }

    fun onToggleFavouriteAppClick(app: AppInfo) {
        viewModelScope.launch {
            if (app.isFavourite) {
                appInfoDao.removeAppFromFavouriteTransaction(
                    app.itemType,
                    app.targetId,
                    app.packageName,
                    app.userHandle,
                    app.orderIndex
                )
            } else {
                val newOrderIndex =
                    (_state.value.favouriteApps.maxOfOrNull { it.orderIndex } ?: 0) + 1
                appInfoDao.addAppToFavourite(
                    app.itemType,
                    app.targetId,
                    app.packageName,
                    app.userHandle,
                    newOrderIndex
                )
            }
        }
    }

    fun onToggleHideClick(app: AppInfo) {
        viewModelScope.launch {
            if (app.isHidden) {
                appInfoDao.removeAppFromHidden(
                    app.itemType,
                    app.targetId,
                    app.packageName,
                    app.userHandle
                )
            } else {
                appInfoDao.addAppToHiddenTransaction(
                    app.itemType,
                    app.targetId,
                    app.packageName,
                    app.userHandle,
                    app.orderIndex
                )
            }
        }
    }

    fun onRenameAppClick(app: AppInfo) {
        _state.update {
            it.copy(
                renameAppDialog = app
            )
        }
    }

    fun onRenameApp(newName: String) {
        val app = _state.value.renameAppDialog ?: return
        onDismissRenameAppDialog()
        viewModelScope.launch {
            val name = newName.trim().takeUnless { it.isBlank() || it == app.appName }.orEmpty()
            appInfoDao.renameApp(
                app.itemType,
                app.targetId,
                app.packageName,
                app.userHandle,
                name
            )
        }
    }

    fun onDismissRenameAppDialog() {
        _state.update {
            it.copy(
                renameAppDialog = null
            )
        }
    }

    fun onLaunchDelayClick(app: AppInfo) {
        _state.update {
            it.copy(launchDelayDialog = app)
        }
    }

    fun onUpdateLaunchDelay(delaySeconds: Int) {
        val app = _state.value.launchDelayDialog ?: return
        onDismissLaunchDelayDialog()
        viewModelScope.launch {
            appInfoDao.updateLaunchDelay(
                itemType = app.itemType,
                targetId = app.targetId,
                packageName = app.packageName,
                userHandle = app.userHandle,
                delaySeconds = delaySeconds
            )
        }
    }

    fun onDismissLaunchDelayDialog() {
        _state.update {
            it.copy(launchDelayDialog = null)
        }
    }

    fun onAppLaunchRequest(app: AppInfo) {
        // Hidden apps need the phone lock check first (MainActivity shows the system prompt)
        if (app.isHidden && _state.value.protectHiddenApps && !appLock.unlocked.value) {
            _state.update { it.copy(pendingProtectedApp = app) }
            return
        }
        launchWithDelayCheck(app)
    }

    fun onProtectedAppAuthResult(success: Boolean) {
        val app = _state.value.pendingProtectedApp ?: return
        _state.update { it.copy(pendingProtectedApp = null) }
        if (success) launchWithDelayCheck(app)
    }

    private fun launchWithDelayCheck(app: AppInfo) {
        if (app.launchDelaySeconds > 0) {
            _state.update {
                it.copy(
                    launchConfirmDialog = PendingAppLaunch(
                        app = app,
                        deadlineElapsedRealtimeMillis = SystemClock.elapsedRealtime() +
                                app.launchDelaySeconds.toLong() * 1_000L
                    )
                )
            }
        } else {
            launchApp(app)
        }
    }

    fun onPreferenceAppLaunchRequest(preference: String): Boolean {
        val target = preference.toAppPreferenceTarget() ?: return false
        val app = _state.value.allApps.find(target::matches) ?: return false

        onAppLaunchRequest(app)
        return true
    }

    fun onConfirmAppLaunch() {
        val app = _state.value.launchConfirmDialog?.app ?: return
        onDismissAppLaunch()
        launchApp(app)
    }

    fun onDismissAppLaunch() {
        _state.update {
            it.copy(launchConfirmDialog = null)
        }
    }

    private fun launchApp(app: AppInfo) {
        launchStats.recordLaunch(app.id)
        when (app.itemType) {
            AppItemType.APP -> applicationContext.launchApp(
                app.packageName,
                app.targetId,
                app.userHandle
            )

            AppItemType.SHORTCUT -> applicationContext.startShortcut(
                app.packageName,
                app.targetId,
                app.userHandle
            )
        }
    }

    fun onConfirmDeleteShortcut(shortcut: AppInfo) {
        if (!shortcut.isShortcut) return
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) {
                shortcutsUtils.deleteShortcut(
                    shortcut.packageName,
                    shortcut.targetId,
                    shortcut.userHandle
                )
            }
            if (removed) {
                appInfoDao.deleteAppTransaction(
                    shortcut.itemType,
                    shortcut.targetId,
                    shortcut.packageName,
                    shortcut.userHandle
                )
                appIconRepository.removeIcon(shortcut.packageName, shortcut.userHandle)
            } else {
                Toast.makeText(
                    applicationContext,
                    applicationContext.getString(R.string.failed_to_delete_shortcut),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onSearchTextChange(searchText: String) {
        val filteredAllApps = getAppsWithSearch(
            searchText = searchText,
            apps = _state.value.allApps,
            includeHiddenApps = _state.value.showHiddenAppsInSearch,
            ignoreSpecialCharacters = _state.value.ignoreSpecialCharacters,
            searchMode = _state.value.searchMode
        )
        _state.update {
            it.copy(
                searchText = searchText,
                filteredAllApps = filteredAllApps,
            )
        }
        if (searchText.isNotBlank() && _state.value.autoOpenApp && filteredAllApps.size == 1) {
            onAppLaunchRequest(filteredAllApps[0])
        }
    }

    fun onKeyboardDone() {
        val state = _state.value
        if (state.searchText.isBlank()) return

        state.filteredAllApps.firstOrNull()?.let(::onAppLaunchRequest)
    }

    /**
     * If searchText is blank, then it should always exclude the favourite and hidden apps from the list.
     *
     * If searchText is not blank, then it should use the "showHiddenApps" flag to decide whether
     * to include the hidden apps in the result.
     * */
    private fun getAppsWithSearch(
        searchText: String,
        apps: List<AppInfo>,
        includeHiddenApps: Boolean,
        ignoreSpecialCharacters: String,
        searchMode: SearchMode
    ): List<AppInfo> {
        val result = if (searchText.isBlank()) {
            apps.filterNot { appInfo ->
                appInfo.isFavourite || appInfo.isHidden
            }
        } else {
            apps.filter { appInfo ->
                // Filter out the special characters from the app name before searching
                val cleanedAppName = appInfo.name.filterNot { ignoreSpecialCharacters.contains(it) }
                (includeHiddenApps || !appInfo.isHidden) &&
                        StringUtils.matchesAppSearch(cleanedAppName, searchText, searchMode)
            }
        }
        return if (sortAppsByUsage) sortByUsage(result) else result
    }

    /** Most opened first; ties keep alphabetical order. The settings item keeps its place. */
    private fun sortByUsage(apps: List<AppInfo>): List<AppInfo> {
        val counts = launchStats.counts.value
        val settingsIndex = apps.indexOfFirst { it.packageName == Constants.MINIMO_SETTINGS_PACKAGE }
        val sorted = apps
            .filterNot { it.packageName == Constants.MINIMO_SETTINGS_PACKAGE }
            .sortedByDescending { counts[it.id] ?: 0 }
        if (settingsIndex < 0) return sorted
        return sorted.toMutableList().apply {
            add(settingsIndex.coerceAtMost(size), apps[settingsIndex])
        }
    }

    private fun migrateFlashlightToButton() {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons ->
                if (buttons.any { it.type == HomeButtonType.FLASHLIGHT }) {
                    buttons
                } else {
                    buttons + HomeButton(type = HomeButtonType.FLASHLIGHT, x = 0.95f, y = 0.03f)
                }
            }
            preferenceHelper.setShowFlashlight(false)
        }
    }

    fun findAppByPreference(preference: String): AppInfo? {
        val target = preference.toAppPreferenceTarget() ?: return null
        return _state.value.allApps.find(target::matches)
    }

    fun onEnterHomeButtonsEditMode() {
        _state.update { it.copy(homeButtonsEditMode = true) }
    }

    fun onExitHomeButtonsEditMode() {
        _state.update { it.copy(homeButtonsEditMode = false) }
    }

    fun onHomeButtonMoved(button: HomeButton, x: Float, y: Float) {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons ->
                buttons.map {
                    if (it.id == button.id) {
                        it.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f))
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun onRemoveHomeButton(button: HomeButton) {
        viewModelScope.launch {
            preferenceHelper.updateHomeButtons { buttons -> buttons.filterNot { it.id == button.id } }
        }
    }

    fun onTimeLimitClick(app: AppInfo) {
        _state.update { it.copy(timeLimitDialog = app) }
    }

    fun onUpdateTimeLimit(minutes: Int) {
        val app = _state.value.timeLimitDialog ?: return
        onDismissTimeLimitDialog()
        timeLimitRepository.setLimit(app.packageName, minutes)
    }

    fun onDismissTimeLimitDialog() {
        _state.update { it.copy(timeLimitDialog = null) }
    }

    /** The carousel moved by one row. */
    fun onCarouselRowPassed() {
        val state = _state.value
        carouselHapticPlayer.play(state.carouselVibration)
        carouselSoundPlayer.play(state.carouselSound, state.carouselSoundVolume / 100f)
    }

    fun onFlashlightClick() {
        if (!flashlightController.toggle()) {
            Toast.makeText(
                applicationContext,
                R.string.flashlight_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun onHomeNoteChanged(note: String) {
        viewModelScope.launch {
            preferenceHelper.setHomeNote(note.trim())
        }
    }

    suspend fun loadWeatherForecast(): WeatherForecast? {
        val state = _state.value
        val latitude = state.weatherLatitude ?: return null
        val longitude = state.weatherLongitude ?: return null
        // Opening the forecast also refreshes the short weather line if it is not fresh
        if (System.currentTimeMillis() - lastWeatherUpdateTime > 5 * 60_000) {
            refreshWeather(force = true)
        }
        val location = latitude to longitude
        val now = System.currentTimeMillis()
        cachedForecast?.let { cached ->
            if (cachedForecastLocation == location && now - cachedForecastTime < 30 * 60_000) {
                return cached
            }
        }
        return weatherRepository.loadForecast(latitude, longitude)?.also { forecast ->
            cachedForecast = forecast
            cachedForecastTime = now
            cachedForecastLocation = location
        }
    }

    /**
     * Per-app screen time for the drawer and for time limits; refreshed at most once a minute
     * unless [force] (e.g. a limit was just changed).
     */
    fun refreshAppScreenTime(force: Boolean = false) {
        val state = _state.value
        if (!state.showAppScreenTime && state.timeLimits.isEmpty()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            !applicationContext.isAppUsagePermissionGranted()
        ) {
            // Access was revoked: turn the option off instead of silently showing nothing
            if (state.showAppScreenTime) {
                viewModelScope.launch { preferenceHelper.setShowAppScreenTime(false) }
            }
            return
        }
        if (!force && System.currentTimeMillis() - lastAppScreenTimeUpdateTime < 60_000) return
        lastAppScreenTimeUpdateTime = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            val usage = screenTimeHelper.getTodayScreenTimePerApp()
            val formatted = usage
                .filterValues { it >= 60_000 }
                .mapValues { (_, millis) -> formatScreenTime(millis) }
            _state.update { it.copy(appScreenTime = formatted, appUsageMillis = usage) }
        }
    }

    private fun formatScreenTime(totalMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(totalMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis) % 60
        return if (hours > 0) {
            applicationContext.getString(R.string.screen_time_hours_minutes, hours, minutes)
        } else {
            applicationContext.getString(R.string.screen_time_minutes, minutes)
        }
    }

    fun refreshWeather(force: Boolean = false) {
        val state = _state.value
        if (!state.showWeather) return
        // Weather changes slowly: update at most every 30 minutes unless forced
        if (!force && System.currentTimeMillis() - lastWeatherUpdateTime < 30 * 60_000) return
        loadWeather(state.weatherLatitude, state.weatherLongitude)
    }

    private fun loadWeather(latitude: Double?, longitude: Double?) {
        if (latitude == null || longitude == null) return
        lastWeatherUpdateTime = System.currentTimeMillis()

        viewModelScope.launch {
            val weather = weatherRepository.loadWeather(latitude, longitude)
            if (weather == null) {
                // Allow a retry on next resume
                lastWeatherUpdateTime = 0L
                return@launch
            }
            val temperature = formatTemperature(weather.temperature)
            val description = applicationContext.getString(weather.descriptionRes)
            _state.update {
                if (it.weatherLatitude == latitude && it.weatherLongitude == longitude) {
                    it.copy(weatherText = "$temperature $description")
                } else {
                    it
                }
            }
        }
    }

    fun refreshScreenTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && applicationContext.isAppUsagePermissionGranted()) {
            // Only continue if 1 minute has been passed since last update
            if (System.currentTimeMillis() - lastScreenTimeUpdateTime < 60_000) return
            // Set on the main thread before the IO work, so two quick calls do not both query
            lastScreenTimeUpdateTime = System.currentTimeMillis()

            viewModelScope.launch(Dispatchers.IO) {
                val totalMillis = screenTimeHelper.getTodayScreenTimeMillis()

                val formattedTime = formatScreenTime(totalMillis)

                _state.update { it.copy(screenTime = formattedTime) }
            }
        } else {
            viewModelScope.launch {
                preferenceHelper.showScreenTimeWidget(false)
            }
        }
    }
}
