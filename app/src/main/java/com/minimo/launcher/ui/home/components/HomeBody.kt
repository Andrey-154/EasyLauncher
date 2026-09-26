package com.minimo.launcher.ui.home.components

import com.minimo.launcher.ui.settings.customisation.components.WeatherCityDialog
import com.minimo.launcher.utils.AppIconAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.material3.LocalTextStyle
import com.minimo.launcher.utils.HomeButton
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimo.launcher.ui.components.ScreenTimeView
import com.minimo.launcher.ui.components.HomeNoteDialog
import com.minimo.launcher.ui.components.HomeNoteView
import com.minimo.launcher.ui.components.TimeAndDateView
import com.minimo.launcher.ui.components.WeatherForecastDialog
import com.minimo.launcher.ui.components.WeatherView
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.ui.home.HomeScreenState
import com.minimo.launcher.ui.home.HomeViewModel
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.ui.theme.LocalCustomTextColor
import com.minimo.launcher.utils.HomeClockMode
import com.minimo.launcher.utils.launchAppInfo
import com.minimo.launcher.utils.openDefaultCalendarApp
import com.minimo.launcher.utils.openDefaultClockApp
import com.minimo.launcher.utils.openDigitalWellbeing
import com.minimo.launcher.utils.openPowerUsageSummary
import com.minimo.launcher.utils.uninstallApp

@Composable
fun HomeBody(
    paddingValues: PaddingValues,
    state: HomeScreenState,
    viewModel: HomeViewModel,
    homeLazyListState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    systemNavigationHeight: Dp,
    statusBarVisible: Boolean,
    navigationBarVisible: Boolean,
    useDarkBottomSheetStatusBarIcons: Boolean,
    useDarkBottomSheetNavigationBarIcons: Boolean,
    onDeleteShortcutClick: (AppInfo) -> Unit,
    onFolderClick: (HomeButton, Offset) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val iconCacheRevision by viewModel.iconCacheRevision.collectAsStateWithLifecycle()

    fun launchPreferredApp(preference: String, fallback: () -> Unit) {
        if (!viewModel.onPreferenceAppLaunchRequest(preference)) {
            fallback()
        }
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val customTextColor = LocalCustomTextColor.current
    val textColor =
        remember(state.enableWallpaper, state.lightTextOnWallpaper, onSurfaceColor, customTextColor) {
            if (state.enableWallpaper && customTextColor == Color.Unspecified) {
                if (state.lightTextOnWallpaper) Color.White else Color.Black
            } else {
                onSurfaceColor
            }
        }

    val textShadow = remember(state.enableWallpaper, state.lightTextOnWallpaper) {
        if (state.enableWallpaper && state.lightTextOnWallpaper) {
            Shadow(
                color = Color.Black.copy(alpha = 0.5f),
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        } else {
            null
        }
    }

    // Keep limit colours on Home up to date (refresh is throttled to once a minute)
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAppScreenTime()
        onPauseOrDispose { }
    }

    var showClockSettings by remember { mutableStateOf(false) }
    if (showClockSettings) {
        ClockQuickSettingsDialog(
            style = state.homeClockStyle,
            mode = state.homeClockMode,
            twentyFourHours = state.twentyFourHourFormat,
            showBattery = state.showBatteryLevel,
            onStyleChange = viewModel::onQuickClockStyle,
            onModeChange = viewModel::onQuickClockMode,
            onTwentyFourHoursChange = viewModel::onQuickTwentyFourHours,
            onShowBatteryChange = viewModel::onQuickShowBattery,
            onHideClock = {
                showClockSettings = false
                viewModel.onQuickHideClock()
            },
            onDismiss = { showClockSettings = false }
        )
    }

    var showCityPicker by remember { mutableStateOf(false) }
    if (showCityPicker) {
        WeatherCityDialog(
            onSearch = viewModel::onWeatherCitySearch,
            onCitySelected = { city ->
                viewModel.onWeatherCitySelected(city)
                showCityPicker = false
            },
            onDetectLocation = viewModel::onDetectWeatherLocation,
            onLocationDetected = { showCityPicker = false },
            onDismiss = { showCityPicker = false }
        )
    }

    var showNoteMenu by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    if (showNoteMenu) {
        NoteQuickDialog(
            hasNote = state.homeNote.isNotEmpty(),
            onEdit = {
                showNoteMenu = false
                showNoteDialog = true
            },
            onClear = {
                showNoteMenu = false
                viewModel.onHomeNoteChanged("")
            },
            onHide = {
                showNoteMenu = false
                viewModel.onQuickHideNote()
            },
            onDismiss = { showNoteMenu = false }
        )
    }
    if (showNoteDialog) {
        HomeNoteDialog(
            currentNote = state.homeNote,
            onSave = { note ->
                viewModel.onHomeNoteChanged(note)
                showNoteDialog = false
            },
            onDismiss = { showNoteDialog = false }
        )
    }

    var showWeatherForecast by remember { mutableStateOf(false) }
    if (showWeatherForecast) {
        WeatherForecastDialog(
            cityName = state.weatherCity,
            loadForecast = viewModel::loadWeatherForecast,
            onDismiss = { showWeatherForecast = false }
        )
    }

    val lazyColumnPadding = remember(systemNavigationHeight, paddingValues) {
        PaddingValues(
            bottom = max(systemNavigationHeight, paddingValues.calculateBottomPadding()) + 16.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(paddingValues)
    ) {
        if (state.showHomeClock || state.showScreenTimeWidget || state.showWeather ||
            state.showHomeNote
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Dimens.APP_HORIZONTAL_SPACING,
                    vertical = 16.dp
                )
            ) {
                if (state.showHomeClock) {
                    TimeAndDateView(
                        horizontalAlignment = state.homeClockAlignment,
                        clockMode = state.homeClockMode,
                        clockStyle = state.homeClockStyle,
                        twentyFourHourFormat = state.twentyFourHourFormat,
                        showBatteryLevel = state.showBatteryLevel,
                        textColor = textColor,
                        textShadow = textShadow,
                        onClockClick = {
                            launchPreferredApp(state.clockAppPreference) {
                                context.openDefaultClockApp()
                            }
                        },
                        onDateClick = {
                            launchPreferredApp(state.calendarAppPreference) {
                                context.openDefaultCalendarApp()
                            }
                        },
                        onBatteryClick = {
                            launchPreferredApp(state.batteryAppPreference) {
                                context.openPowerUsageSummary()
                            }
                        },
                        onLongClick = { showClockSettings = true }
                    )
                }

                if (state.showWeather) {
                    if (state.showHomeClock && state.weatherText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    WeatherView(
                        horizontalAlignment = state.homeClockAlignment,
                        weatherText = state.weatherText,
                        cityMissing = state.weatherLatitude == null,
                        refreshWeather = viewModel::refreshWeather,
                        onClick = {
                            if (state.weatherLatitude != null) showWeatherForecast = true
                        },
                        onLongClick = { showCityPicker = true },
                        textColor = textColor,
                        textShadow = textShadow
                    )
                }

                if (state.showHomeNote) {
                    Spacer(modifier = Modifier.height(8.dp))

                    HomeNoteView(
                        horizontalAlignment = state.homeClockAlignment,
                        note = state.homeNote,
                        onClick = { showNoteDialog = true },
                        onLongClick = { showNoteMenu = true },
                        textColor = textColor,
                        textShadow = textShadow
                    )
                }

                if (state.showScreenTimeWidget && state.screenTime.isNotEmpty()) {
                    if (state.showHomeClock || state.showWeather || state.showHomeNote) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ScreenTimeView(
                        horizontalAlignment = state.homeClockAlignment,
                        screenTime = state.screenTime,
                        refreshScreenTime = viewModel::refreshScreenTime,
                        onClick = {
                            launchPreferredApp(state.screenTimeAppPreference) {
                                context.openDigitalWellbeing()
                            }
                        },
                        textColor = textColor,
                        textShadow = textShadow
                    )
                }
            }
        }

        // One row of the home list; shared by the normal list and the carousel
        val homeAppItem: @Composable (AppInfo, Modifier) -> Unit = { appInfo, itemModifier ->
                val textSize = state.homeTextSize.sp
                val appIconSizeScale = state.appIconSizePercent / 100f
                val iconSizePx = with(LocalDensity.current) {
                    appIconSizeFor(textSize, appIconSizeScale).roundToPx()
                }
                val appIcon by produceState<ImageBitmap?>(
                    initialValue = null,
                    key1 = state.showAppIconInHome,
                    key2 = appInfo.id,
                    key3 = iconSizePx to iconCacheRevision
                ) {
                    if (state.showAppIconInHome) {
                        value = viewModel.loadAppIcon(appInfo, iconSizePx)
                    }
                }

                // On Home only apps with a limit show their time
                val decoration = usageDecoration(
                    packageName = appInfo.packageName,
                    isShortcut = appInfo.isShortcut,
                    state = state,
                    showUsageWithoutLimit = false
                )

                AppNameItem(
                    modifier = itemModifier,
                    appName = appInfo.name,
                    isFavourite = appInfo.isFavourite,
                    isHidden = appInfo.isHidden,
                    isShortcut = appInfo.isShortcut,
                    isWorkProfile = appInfo.isWorkProfile,
                    onClick = { viewModel.onAppLaunchRequest(appInfo) },
                    onToggleFavouriteClick = {
                        viewModel.onToggleFavouriteAppClick(
                            appInfo
                        )
                    },
                    onRenameClick = { viewModel.onRenameAppClick(appInfo) },
                    onToggleHideClick = { viewModel.onToggleHideClick(appInfo) },
                    onAppInfoClick = { context.launchAppInfo(appInfo) },
                    onLaunchDelayClick = { viewModel.onLaunchDelayClick(appInfo) },
                    appsArrangement = state.appsArrangementHorizontal,
                    textSize = textSize,
                    onUninstallClick = { context.uninstallApp(appInfo) },
                    onDeleteShortcutClick = { onDeleteShortcutClick(appInfo) },
                    showNotificationDot = appInfo.showNotificationDot,
                    compactTouchArea = state.compactAppTouchArea,
                    showAppIcon = state.showAppIconInHome,
                    appIcon = appIcon,
                    appIconSizeScale = appIconSizeScale,
                    appIconAlignment = state.homeAppIconAlignment,
                    verticalPadding = state.homeAppVerticalPadding.dp,
                    bottomSheetStatusBarVisible = statusBarVisible,
                    bottomSheetNavigationBarVisible = navigationBarVisible,
                    useDarkBottomSheetStatusBarIcons = useDarkBottomSheetStatusBarIcons,
                    useDarkBottomSheetNavigationBarIcons = useDarkBottomSheetNavigationBarIcons,
                    textColor = textColor,
                    shadow = textShadow,
                    secondaryText = decoration.text,
                    usageColor = decoration.color,
                    colorName = decoration.colorName,
                    onTimeLimitClick = { viewModel.onTimeLimitClick(appInfo) }
                )
            }

        // Favourites plus the folders placed "in the list", each at its chosen position
        val homeEntries = remember(state.favouriteApps, state.homeButtons) {
            val entries = state.favouriteApps.map<AppInfo, HomeEntry> { HomeEntry.App(it) }.toMutableList()
            state.homeButtons.filter { it.isListFolder }
                .sortedBy { it.listPosition }
                .forEach { folder ->
                    entries.add(folder.listPosition.coerceIn(0, entries.size), HomeEntry.Folder(folder))
                }
            entries.toList()
        }
        val homeEntryItem: @Composable (HomeEntry, Modifier) -> Unit = { entry, itemModifier ->
            when (entry) {
                is HomeEntry.App -> homeAppItem(entry.app, itemModifier)
                is HomeEntry.Folder -> FolderListRow(
                    modifier = itemModifier,
                    folder = entry.folder,
                    textSize = state.homeTextSize.sp,
                    textColor = textColor,
                    textShadow = textShadow,
                    appsArrangement = state.appsArrangementHorizontal,
                    verticalPadding = state.homeAppVerticalPadding.dp,
                    appIconSize = if (state.showAppIconInHome) {
                        appIconSizeFor(state.homeTextSize.sp, state.appIconSizePercent / 100f)
                    } else {
                        null
                    },
                    iconOnRight = state.homeAppIconAlignment == AppIconAlignment.Right,
                    findApp = viewModel::findAppByPreference,
                    loadAppIcon = { app, sizePx -> viewModel.loadAppIcon(app, sizePx) },
                    onClick = { center -> onFolderClick(entry.folder, center) }
                )
            }
        }

        // "Max apps on Home": an endless carousel showing exactly N whole rows
        val carouselRows = state.maxHomeApps.takeIf {
            state.limitHomeApps && homeEntries.size > it
        }

        // Row height of the carousel, the same for apps and folders:
        // text line (or the app icon if bigger) + top and bottom padding
        val homeTextSize = state.homeTextSize.sp
        val density = LocalDensity.current
        val carouselRowHeight = with(density) {
            val line = (homeTextSize * 1.2).toDp()
            val icon = if (state.showAppIconInHome) {
                appIconSizeFor(homeTextSize, state.appIconSizePercent / 100f)
            } else {
                0.dp
            }
            max(line, icon) + state.homeAppVerticalPadding.dp * 2
        }

        // "Compact touch area": the carousel is only as wide as the longest name,
        // so it can be scrolled only there
        val homeIconSize = appIconSizeFor(homeTextSize, state.appIconSizePercent / 100f)
        val textMeasurer = rememberTextMeasurer()
        val textStyle = LocalTextStyle.current.copy(fontSize = homeTextSize)
        val compactCarouselWidth = if (carouselRows != null && state.compactAppTouchArea) {
            remember(homeEntries, textStyle, state.showAppIconInHome, state.appIconSizePercent) {
                val widest = homeEntries.maxOfOrNull { entry ->
                    val name = when (entry) {
                        is HomeEntry.App -> entry.app.name
                        is HomeEntry.Folder -> entry.folder.name
                    }
                    textMeasurer.measure(name, textStyle, maxLines = 1).size.width
                } ?: 0
                with(density) {
                    val leading = if (state.showAppIconInHome) {
                        homeIconSize + Dimens.APP_ICON_LABEL_SPACING
                    } else {
                        // room for a folder glyph / work profile icon / notification dot
                        (homeTextSize * 1.1f).toDp() + 10.dp
                    }
                    widest.toDp() + leading + Dimens.APP_HORIZONTAL_SPACING * 2 + 16.dp
                }
            }
        } else {
            null
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(
                horizontalBias = when (state.appsArrangementHorizontal) {
                    Arrangement.Center -> 0f
                    Arrangement.End -> 1f
                    else -> -1f
                },
                verticalBias = when (state.appsArrangementVertical) {
                    Arrangement.Top -> -1f
                    Arrangement.Bottom -> 1f
                    else -> 0f
                }
            )
        ) {
            if (carouselRows != null) {
                // Empty space around the carousel keeps the swipes for the drawer / notifications.
                // The carousel is not inside this nested scroll, so spinning it never opens them.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .nestedScroll(nestedScrollConnection)
                        .scrollable(
                            state = rememberScrollableState { 0f },
                            orientation = Orientation.Vertical
                        )
                )

                HomeAppsCarousel(
                    apps = homeEntries,
                    visibleCount = carouselRows,
                    rowHeight = carouselRowHeight,
                    width = compactCarouselWidth,
                    onRowPassed = viewModel::onCarouselRowPassed,
                    resetToStart = state.carouselReset,
                    resetEvents = viewModel.homePressedEvents,
                    transformOriginX = when (state.appsArrangementHorizontal) {
                        Arrangement.Center -> 0.5f
                        Arrangement.End -> 1f
                        else -> 0f
                    },
                    modifier = Modifier.padding(bottom = lazyColumnPadding.calculateBottomPadding()),
                    itemContent = homeEntryItem
                )
            } else {
                LazyColumn(
                    state = homeLazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = lazyColumnPadding,
                    verticalArrangement = state.appsArrangementVertical
                ) {
                    items(items = homeEntries, key = { it.key }) { entry ->
                        homeEntryItem(entry, Modifier.animateItem())
                    }
                }
            }
        }
    }
}
