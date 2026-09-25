package com.minimo.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.utils.AppIconAlignment
import com.minimo.launcher.utils.Constants
import com.minimo.launcher.utils.FastScrollerAlignment
import com.minimo.launcher.utils.HomeClockMode
import com.minimo.launcher.utils.HomeClockStyle
import com.minimo.launcher.utils.MinimoSettingsPosition
import com.minimo.launcher.utils.SearchMode
import com.minimo.launcher.utils.TimeLimitRepository
import com.minimo.launcher.utils.HomeButton
import com.minimo.launcher.utils.HomeButtonSize
import com.minimo.launcher.utils.HomeButtonStyle

data class PendingAppLaunch(
    val app: AppInfo,
    val deadlineElapsedRealtimeMillis: Long
)

data class HomeScreenState(
    val initialLoaded: Boolean = false,
    val favouriteApps: List<AppInfo> = emptyList(),
    val allApps: List<AppInfo> = emptyList(),
    val filteredAllApps: List<AppInfo> = emptyList(),
    val renameAppDialog: AppInfo? = null,
    val launchDelayDialog: AppInfo? = null,
    val launchConfirmDialog: PendingAppLaunch? = null,
    val searchText: String = "",
    val appsArrangementHorizontal: Arrangement.Horizontal = Arrangement.Start,
    val drawerAppsArrangementHorizontal: Arrangement.Horizontal = Arrangement.Start,
    val appsArrangementVertical: Arrangement.Vertical = Arrangement.Center,
    val showHomeClock: Boolean = false,
    val homeClockAlignment: Alignment.Horizontal = Alignment.Start,
    val homeTextSize: Int = Constants.DEFAULT_HOME_TEXT_SIZE,
    val autoOpenKeyboardAllApps: Boolean = false,
    val homeClockMode: HomeClockMode = HomeClockMode.Full,
    val doubleTapToLock: Boolean = false,
    val twentyFourHourFormat: Boolean = false,
    val showBatteryLevel: Boolean = false,
    val showHiddenAppsInSearch: Boolean = true,
    val drawerSearchBarAtBottom: Boolean = false,
    val showAppIconInHome: Boolean = false,
    val showAppIconInDrawer: Boolean = false,
    val homeAppIconAlignment: AppIconAlignment = AppIconAlignment.Left,
    val drawerAppIconAlignment: AppIconAlignment = AppIconAlignment.Left,
    val appIconSizePercent: Int = Constants.DEFAULT_APP_ICON_SIZE_PERCENT,
    val applyHomeAppSizeToAllApps: Boolean = false,
    val autoOpenApp: Boolean = false,
    val homeAppVerticalPadding: Int = Constants.DEFAULT_HOME_VERTICAL_PADDING,
    val ignoreSpecialCharacters: String = "",
    val searchMode: SearchMode = SearchMode.Contains,
    val searchBarBackground: Boolean = false,
    val searchPreferencesLoaded: Boolean = false,
    val searchBarBorderPercent: Int = Constants.DEFAULT_SEARCH_BAR_BORDER_PERCENT,
    val hideAppDrawerSearch: Boolean = false,
    val hideSettingsIcon: Boolean = false,
    val showScreenTimeWidget: Boolean = false,
    val screenTime: String = "",
    val enableWallpaper: Boolean = false,
    val enableWallpaperOnDrawer: Boolean = false,
    val lightTextOnWallpaper: Boolean = true,
    val clockAppPreference: String = "",
    val batteryAppPreference: String = "",
    val calendarAppPreference: String = "",
    val screenTimeAppPreference: String = "",
    val swipeLeftAppPreference: String = "",
    val swipeRightAppPreference: String = "",
    val minimoSettingsPosition: MinimoSettingsPosition = MinimoSettingsPosition.Auto,
    val keyboardOpenDelay: Long = Constants.DEFAULT_KEYBOARD_OPEN_DELAY,
    val enableFastScroller: Boolean = false,
    val fastScrollerAlignment: FastScrollerAlignment = FastScrollerAlignment.Right,
    val backOpensAppDrawer: Boolean = true,
    val compactAppTouchArea: Boolean = false,
    val keyboardDoneOpensFirstApp: Boolean = false,
    val showWeather: Boolean = false,
    val weatherCity: String = "",
    val weatherLatitude: Double? = null,
    val weatherLongitude: Double? = null,
    val weatherText: String = "",
    val homeClockStyle: HomeClockStyle = HomeClockStyle.Normal,
    val showAppScreenTime: Boolean = false,
    /** Package name -> formatted screen time today, e.g. "1 ч 20 мин" */
    val appScreenTime: Map<String, String> = emptyMap(),
    val showHomeNote: Boolean = false,
    val homeNote: String = "",
    val sortAppsByUsage: Boolean = false,
    val showFlashlight: Boolean = false,
    val timeLimitDialog: AppInfo? = null,
    /** Package name -> daily limit in minutes */
    val timeLimits: Map<String, Int> = emptyMap(),
    /** Package name -> foreground time today */
    val appUsageMillis: Map<String, Long> = emptyMap(),
    val limitColorTimeOnly: Boolean = true,
    val limitWarningColor: Int = TimeLimitRepository.DEFAULT_WARNING_COLOR,
    val limitExceededColor: Int = TimeLimitRepository.DEFAULT_EXCEEDED_COLOR,
    val homeButtons: List<HomeButton> = emptyList(),
    val homeButtonSize: HomeButtonSize = HomeButtonSize.Medium,
    val homeButtonStyle: HomeButtonStyle = HomeButtonStyle.Outline,
    val homeButtonsEditMode: Boolean = false
)
