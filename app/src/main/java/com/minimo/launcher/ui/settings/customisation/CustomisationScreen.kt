package com.minimo.launcher.ui.settings.customisation

import com.minimo.launcher.ui.settings.customisation.components.CarouselFeedbackSettings
import androidx.compose.foundation.layout.PaddingValues
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.minimo.launcher.R
import com.minimo.launcher.ui.settings.app_picker.AppPickerDialog
import com.minimo.launcher.ui.settings.customisation.components.AppIconAlignmentDropdown
import com.minimo.launcher.ui.settings.customisation.components.AppIconSizeSlider
import com.minimo.launcher.ui.settings.customisation.components.AppSizeSlider
import com.minimo.launcher.ui.settings.customisation.components.AppsAlignmentHorizontalDropdown
import com.minimo.launcher.ui.settings.customisation.components.AppsAlignmentVerticalDropdown
import com.minimo.launcher.ui.settings.customisation.components.ClockAlignmentDropdown
import com.minimo.launcher.ui.settings.customisation.components.ClockModeDropdown
import com.minimo.launcher.ui.settings.customisation.components.ColorPickerItem
import com.minimo.launcher.ui.settings.customisation.components.MaxHomeAppsSlider
import com.minimo.launcher.ui.settings.customisation.components.ClockStyleDropdown
import com.minimo.launcher.ui.settings.customisation.components.DimPercentageSlider
import com.minimo.launcher.ui.settings.customisation.components.EnableAccessibilityDialog
import com.minimo.launcher.ui.settings.customisation.components.EnableAppUsageDialog
import com.minimo.launcher.ui.settings.customisation.components.EnableNotificationsDialog
import com.minimo.launcher.ui.settings.customisation.components.EnableSetWallpaperToThemeColorDialog
import com.minimo.launcher.ui.settings.customisation.components.FastScrollerAlignmentDropdown
import com.minimo.launcher.ui.settings.customisation.components.FontDropdown
import com.minimo.launcher.ui.settings.customisation.components.IgnoreSpecialCharacters
import com.minimo.launcher.ui.settings.customisation.components.WeatherCityItem
import com.minimo.launcher.ui.settings.customisation.components.MinimoSettingsPositionDropdown
import com.minimo.launcher.ui.settings.customisation.components.OrientationDropdown
import com.minimo.launcher.ui.settings.customisation.components.SearchBarBorderSlider
import com.minimo.launcher.ui.settings.customisation.components.SearchModeDropdown
import com.minimo.launcher.ui.settings.customisation.components.ThemeDropdown
import com.minimo.launcher.ui.settings.customisation.components.ToggleItem
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.ui.theme.ThemeMode
import com.minimo.launcher.utils.AndroidUtils
import com.minimo.launcher.utils.AppIconAlignment
import com.minimo.launcher.utils.Constants
import com.minimo.launcher.utils.Constants.KEYBOARD_OPEN_DELAY_RANGE
import com.minimo.launcher.utils.FastScrollerAlignment
import com.minimo.launcher.utils.HomeAppsAlignmentHorizontal
import com.minimo.launcher.utils.HomeAppsAlignmentVertical
import com.minimo.launcher.utils.HomeClockAlignment
import com.minimo.launcher.utils.HomeClockMode
import com.minimo.launcher.utils.MinimoSettingsPosition
import com.minimo.launcher.utils.ScreenOrientation
import com.minimo.launcher.utils.SearchMode
import com.minimo.launcher.utils.StringUtils
import com.minimo.launcher.utils.TimeLimitRepository
import com.minimo.launcher.utils.hasLockScreenPermission
import com.minimo.launcher.utils.isAppUsagePermissionGranted
import com.minimo.launcher.utils.isNotificationPermissionGranted
import com.minimo.launcher.utils.openNotificationSettings
import com.minimo.launcher.utils.openUsageAccessSettings
import com.minimo.launcher.utils.removeLockScreenPermission
import com.minimo.launcher.utils.requestLockScreenPermission
import kotlinx.coroutines.android.awaitFrame

@Composable
fun CustomisationScreen(
    viewModel: CustomisationViewModel,
    onBackClick: () -> Unit,
    /** Filter from the main settings search field; empty shows everything. */
    searchQuery: String = "",
    /** Only the list, without the top bar (shown inside the main settings search). */
    embedded: Boolean = false,
    /** One page of the settings; null = everything. */
    section: SettingsSection? = null
) {
    // While searching, also show settings that normally appear only after their parent is enabled
    val isSearching = searchQuery.isNotBlank()
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEnableAccessibilityDialog by remember { mutableStateOf(false) }
    var hasLockScreenPermission by remember {
        mutableStateOf(context.hasLockScreenPermission())
    }
    var showEnableNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showEnableAppUsagePermissionDialog by remember { mutableStateOf(false) }
    var showSetWallpaperToThemeColorDialog by remember { mutableStateOf(false) }

    var showClockAppPicker by remember { mutableStateOf(false) }
    var showBatteryAppPicker by remember { mutableStateOf(false) }
    var showCalendarAppPicker by remember { mutableStateOf(false) }
    var showScreenTimeAppPicker by remember { mutableStateOf(false) }

    var showSwipeLeftAppPicker by remember { mutableStateOf(false) }
    var showSwipeRightAppPicker by remember { mutableStateOf(false) }

    var showKeyboardOpenDelayDialog by remember { mutableStateOf(false) }
    var keyboardOpenDelayInput by remember { mutableStateOf(state.keyboardOpenDelay.toString()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            hasLockScreenPermission = context.hasLockScreenPermission()

            if (!context.isNotificationPermissionGranted()) {
                viewModel.onNotificationPermissionNotGrantedOnStarted()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !context.isAppUsagePermissionGranted()) {
                viewModel.onAppUsagePermissionNotGrantedOnStarted()
            }
        }
    }

    val content: @Composable (PaddingValues) -> Unit = content@{ paddingValues ->
        if (!state.initialLoaded) return@content

        CompositionLocalProvider(
            LocalSettingsQuery provides searchQuery,
            LocalSettingsSection provides section
        ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            InSection(SettingsSection.Behavior) {
                OrientationDropdown(
                    selectedOption = StringUtils.screenOrientationText(
                        context = context,
                        orientation = state.screenOrientation
                    ),
                    options = listOf(
                        ScreenOrientation.Portrait to StringUtils.screenOrientationText(
                            context,
                            ScreenOrientation.Portrait
                        ),
                        ScreenOrientation.Landscape to StringUtils.screenOrientationText(
                            context,
                            ScreenOrientation.Landscape
                        ),
                        ScreenOrientation.Auto to StringUtils.screenOrientationText(
                            context,
                            ScreenOrientation.Auto
                        )
                    ),
                    onOptionSelected = viewModel::onScreenOrientationChanged
                )
            }

            InSection(SettingsSection.Look) {
                FontDropdown(
                    selectedFont = state.fontPreference,
                    onFontSelected = viewModel::onFontPreferenceChanged
                )
            }
            
            InSection(SettingsSection.Look) {
                ThemeDropdown(
                    selectedOption = StringUtils.themeModeText(
                        context = context,
                        mode = state.themeMode
                    ),
                    options = listOf(
                        ThemeMode.System to StringUtils.themeModeText(
                            context,
                            ThemeMode.System
                        ),
                        ThemeMode.Dark to StringUtils.themeModeText(
                            context,
                            ThemeMode.Dark
                        ),
                        ThemeMode.Light to StringUtils.themeModeText(
                            context,
                            ThemeMode.Light
                        )
                    ),
                    onOptionSelected = viewModel::onThemeModeChanged
                )
            }

            InSection(SettingsSection.Look) {
                ToggleItem(
                    title = stringResource(R.string.black_theme),
                    subtitle = stringResource(R.string.applied_only_when_the_app_theme_is_in_dark_mode),
                    isChecked = state.blackTheme,
                    onToggleClick = viewModel::onToggleBlackTheme
                )
            }

            InSection(SettingsSection.Look) {
                if (AndroidUtils.isDynamicThemeSupported()) {
                    SettingsSpacer(4.dp)

                    ToggleItem(
                        title = stringResource(R.string.dynamic_colours),
                        subtitle = stringResource(R.string.adapt_theme_colours_based_on_system_settings),
                        isChecked = state.dynamicTheme,
                        onToggleClick = viewModel::onToggleDynamicTheme
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Look) {
                if (LocalSettingsQuery.current.isBlank()) {
                    Text(
                        text = stringResource(R.string.custom_colors_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING)
                    )
                }
            }

            InSection(SettingsSection.Look) {
                ColorPickerItem(
                    title = stringResource(R.string.custom_background_color),
                    color = state.customBackgroundColor,
                    onColorSelected = viewModel::onCustomBackgroundColorChanged
                )
            }

            InSection(SettingsSection.Look) {
                ColorPickerItem(
                    title = stringResource(R.string.custom_text_color),
                    color = state.customTextColor,
                    onColorSelected = viewModel::onCustomTextColorChanged
                )
            }

            InSection(SettingsSection.Look) {
                ColorPickerItem(
                    title = stringResource(R.string.custom_accent_color),
                    color = state.customAccentColor,
                    onColorSelected = viewModel::onCustomAccentColorChanged
                )
            }

            InSection(SettingsSection.Look) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ToggleItem(
                        title = stringResource(R.string.blur_behind),
                        subtitle = stringResource(R.string.blur_behind_description),
                        isChecked = state.blurBehind,
                        onToggleClick = viewModel::onToggleBlurBehind
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Look) {
                ToggleItem(
                    title = stringResource(R.string.enable_wallpaper),
                    isChecked = state.enableWallpaper,
                    onToggleClick = viewModel::onToggleEnableWallpaper
                )
            }

            InSection(SettingsSection.Look) {
                ToggleItem(
                    title = stringResource(R.string.enable_wallpaper_on_drawer),
                    isChecked = state.enableWallpaperOnDrawer,
                    onToggleClick = viewModel::onToggleEnableWallpaperOnDrawer
                )
            }

            InSection(SettingsSection.Look) {
                if (isSearching || (state.enableWallpaper || state.enableWallpaperOnDrawer)) {
                    ToggleItem(
                        title = stringResource(R.string.light_text_on_wallpaper),
                        isChecked = state.lightTextOnWallpaper,
                        onToggleClick = viewModel::onToggleLightTextOnWallpaper
                    )

                    ToggleItem(
                        title = stringResource(R.string.dim_wallpaper),
                        isChecked = state.dimWallpaper,
                        onToggleClick = viewModel::onToggleDimWallpaper
                    )

                    if (isSearching || state.dimWallpaper) {
                        SettingsSpacer(8.dp)

                        DimPercentageSlider(
                            dimPercentage = state.dimWallpaperPercentage,
                            onDimPercentageChanged = viewModel::onDimWallpaperPercentageChanged
                        )
                    }
                }
            }

            InSection(SettingsSection.Look) {
                ToggleItem(
                    title = stringResource(R.string.set_wallpaper_to_theme_color),
                    isChecked = state.setWallpaperToThemeColor,
                    onToggleClick = {
                        if (state.setWallpaperToThemeColor) {
                            viewModel.onToggleSetWallpaperToThemeColor()
                        } else {
                            showSetWallpaperToThemeColorDialog = true
                        }
                    }
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsSpacer(8.dp)

            InSection(SettingsSection.Home) {
                ToggleItem(
                    title = stringResource(R.string.app_icon_in_home),
                    isChecked = state.showAppIconInHome,
                    onToggleClick = viewModel::onToggleShowAppIconInHome
                )
            }

            InSection(SettingsSection.Home) {
                if (isSearching || state.showAppIconInHome) {
                    AppIconAlignmentDropdown(
                        titleRes = R.string.home_icon_alignment,
                        selectedOption = StringUtils.appIconAlignmentText(
                            context,
                            state.homeAppIconAlignment
                        ),
                        options = listOf(
                            AppIconAlignment.Left to StringUtils.appIconAlignmentText(
                                context,
                                AppIconAlignment.Left
                            ),
                            AppIconAlignment.Right to StringUtils.appIconAlignmentText(
                                context,
                                AppIconAlignment.Right
                            )
                        ),
                        onOptionSelected = viewModel::onHomeAppIconAlignmentChanged
                    )
                }
            }

            InSection(SettingsSection.Drawer) {
                ToggleItem(
                    title = stringResource(R.string.app_icon_in_drawer),
                    isChecked = state.showAppIconInDrawer,
                    onToggleClick = viewModel::onToggleShowAppIconInDrawer
                )
            }

            InSection(SettingsSection.Drawer) {
                if (isSearching || state.showAppIconInDrawer) {
                    AppIconAlignmentDropdown(
                        titleRes = R.string.drawer_icon_alignment,
                        selectedOption = StringUtils.appIconAlignmentText(
                            context,
                            state.drawerAppIconAlignment
                        ),
                        options = listOf(
                            AppIconAlignment.Left to StringUtils.appIconAlignmentText(
                                context,
                                AppIconAlignment.Left
                            ),
                            AppIconAlignment.Right to StringUtils.appIconAlignmentText(
                                context,
                                AppIconAlignment.Right
                            )
                        ),
                        onOptionSelected = viewModel::onDrawerAppIconAlignmentChanged
                    )
                }
            }

            InSection(SettingsSection.Home) {
                if (isSearching || (state.showAppIconInHome || state.showAppIconInDrawer)) {
                    SettingsSpacer(8.dp)

                    AppIconSizeSlider(
                        appIconSizePercent = state.appIconSizePercent,
                        onAppIconSizePercentChanged = viewModel::onAppIconSizePercentChanged
                    )
                }
            }

            SettingsSpacer(8.dp)

            InSection(SettingsSection.Home) {
                AppSizeSlider(
                    homeTextSize = state.homeTextSize,
                    onHomeTextSizeChanged = viewModel::onHomeTextSizeChanged,
                    homeAppVerticalPadding = state.homeAppVerticalPadding,
                    onHomeVerticalPaddingChanged = viewModel::onHomeVerticalPaddingChanged,
                    showAppIcon = state.showAppIconInHome,
                    appIconSizeScale = state.appIconSizePercent / 100f,
                    appIconAlignment = state.homeAppIconAlignment
                )
            }

            SettingsSpacer(8.dp)

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Drawer) {
                ToggleItem(
                    title = stringResource(R.string.apply_to_all_apps),
                    subtitle = stringResource(R.string.apply_the_home_app_size_to_all_apps_in_the_app_drawer),
                    isChecked = state.applyHomeAppSizeToAllApps,
                    onToggleClick = viewModel::onToggleApplyHomeAppSizeToAllApps
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Home) {
                AppsAlignmentHorizontalDropdown(
                    selectedOption = StringUtils.homeAppsAlignmentHorizontalText(
                        context = context,
                        alignment = state.homeAppsAlignmentHorizontal
                    ),
                    options = listOf(
                        HomeAppsAlignmentHorizontal.Start to StringUtils.homeAppsAlignmentHorizontalText(
                            context,
                            HomeAppsAlignmentHorizontal.Start
                        ),
                        HomeAppsAlignmentHorizontal.Center to StringUtils.homeAppsAlignmentHorizontalText(
                            context,
                            HomeAppsAlignmentHorizontal.Center
                        ),
                        HomeAppsAlignmentHorizontal.End to StringUtils.homeAppsAlignmentHorizontalText(
                            context,
                            HomeAppsAlignmentHorizontal.End
                        ),
                    ),
                    onOptionSelected = viewModel::onHomeAppsAlignmentHorizontalChanged
                )
            }

            InSection(SettingsSection.Home) {
                AppsAlignmentVerticalDropdown(
                    selectedOption = StringUtils.homeAppsAlignmentVerticalText(
                        context = context,
                        alignment = state.homeAppsAlignmentVertical
                    ),
                    options = listOf(
                        HomeAppsAlignmentVertical.Top to StringUtils.homeAppsAlignmentVerticalText(
                            context,
                            HomeAppsAlignmentVertical.Top
                        ),
                        HomeAppsAlignmentVertical.Center to StringUtils.homeAppsAlignmentVerticalText(
                            context,
                            HomeAppsAlignmentVertical.Center
                        ),
                        HomeAppsAlignmentVertical.Bottom to StringUtils.homeAppsAlignmentVerticalText(
                            context,
                            HomeAppsAlignmentVertical.Bottom
                        ),
                    ),
                    onOptionSelected = viewModel::onHomeAppsAlignmentVerticalChanged
                )
            }

            InSection(SettingsSection.Home) {
                ToggleItem(
                    title = stringResource(R.string.limit_home_apps),
                    subtitle = stringResource(R.string.limit_home_apps_description),
                    isChecked = state.limitHomeApps,
                    onToggleClick = viewModel::onToggleLimitHomeApps
                )
            }

            InSection(SettingsSection.Home) {
                if (isSearching || state.limitHomeApps) {
                    SettingsSpacer(8.dp)

                    MaxHomeAppsSlider(
                        maxHomeApps = state.maxHomeApps,
                        onMaxHomeAppsChanged = viewModel::onMaxHomeAppsChanged
                    )

                    ToggleItem(
                        title = stringResource(R.string.carousel_reset),
                        subtitle = stringResource(R.string.carousel_reset_description),
                        isChecked = state.carouselReset,
                        onToggleClick = viewModel::onToggleCarouselReset
                    )

                    CarouselFeedbackSettings(
                        vibration = state.carouselVibration,
                        sound = state.carouselSound,
                        soundVolume = state.carouselSoundVolume,
                        onVibrationChanged = viewModel::onCarouselVibrationChanged,
                        onSoundChanged = viewModel::onCarouselSoundChanged,
                        onSoundVolumeChanged = viewModel::onCarouselSoundVolumeChanged
                    )
                }
            }

            InSection(SettingsSection.Drawer) {
                AppsAlignmentHorizontalDropdown(
                    titleRes = R.string.drawer_apps_alignment_horizontal,
                    selectedOption = StringUtils.homeAppsAlignmentHorizontalText(
                        context = context,
                        alignment = state.drawerAppsAlignmentHorizontal
                    ),
                    options = listOf(
                        HomeAppsAlignmentHorizontal.Start to StringUtils.homeAppsAlignmentHorizontalText(
                            context,
                            HomeAppsAlignmentHorizontal.Start
                        ),
                        HomeAppsAlignmentHorizontal.Center to StringUtils.homeAppsAlignmentHorizontalText(
                            context,
                            HomeAppsAlignmentHorizontal.Center
                        ),
                        HomeAppsAlignmentHorizontal.End to StringUtils.homeAppsAlignmentHorizontalText(
                            context,
                            HomeAppsAlignmentHorizontal.End
                        ),
                    ),
                    onOptionSelected = viewModel::onDrawerAppsAlignmentHorizontalChanged
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Widgets) {
                ToggleItem(
                    title = stringResource(R.string.show_home_clock),
                    isChecked = state.showHomeClock,
                    onToggleClick = viewModel::onToggleShowHomeClock
                )
            }
            InSection(SettingsSection.Widgets) {
                if (isSearching || state.showHomeClock) {
                    SettingsSpacer(4.dp)

                    ClockAlignmentDropdown(
                        selectedOption = StringUtils.homeClockAlignmentText(
                            context = context,
                            alignment = state.homeClockAlignment
                        ),
                        options = listOf(
                            HomeClockAlignment.Start to StringUtils.homeClockAlignmentText(
                                context,
                                HomeClockAlignment.Start
                            ),
                            HomeClockAlignment.Center to StringUtils.homeClockAlignmentText(
                                context,
                                HomeClockAlignment.Center
                            ),
                            HomeClockAlignment.End to StringUtils.homeClockAlignmentText(
                                context,
                                HomeClockAlignment.End
                            ),
                        ),
                        onOptionSelected = viewModel::onHomeClockAlignmentChanged
                    )

                    SettingsSpacer(4.dp)

                    ClockModeDropdown(
                        selectedOption = StringUtils.homeClockModeText(
                            context = context,
                            mode = state.homeClockMode
                        ),
                        options = listOf(
                            HomeClockMode.Full to StringUtils.homeClockModeText(
                                context,
                                HomeClockMode.Full
                            ),
                            HomeClockMode.TimeOnly to StringUtils.homeClockModeText(
                                context,
                                HomeClockMode.TimeOnly
                            ),
                            HomeClockMode.DateOnly to StringUtils.homeClockModeText(
                                context,
                                HomeClockMode.DateOnly
                            ),
                        ),
                        onOptionSelected = viewModel::onHomeClockModeChanged
                    )

                    if (isSearching || state.homeClockMode != HomeClockMode.DateOnly) {
                        ClockStyleDropdown(
                            selectedStyle = state.homeClockStyle,
                            onStyleSelected = viewModel::onHomeClockStyleChanged
                        )
                    }

                    SettingsSpacer(4.dp)

                    ToggleItem(
                        title = stringResource(R.string.twenty_four_hour_format),
                        isChecked = state.twentyFourHourFormat,
                        onToggleClick = viewModel::onToggleTwentyFourHourFormat
                    )

                    if (state.homeClockMode == HomeClockMode.DateOnly ||
                        state.homeClockMode == HomeClockMode.Full
                    ) {
                        SettingsSpacer(4.dp)

                        ToggleItem(
                            title = stringResource(R.string.show_battery_level),
                            isChecked = state.showBatteryLevel,
                            onToggleClick = viewModel::onToggleShowBatteryLevel
                        )

                        SettingsSpacer(4.dp)

                        AppSelectionItem(
                            title = stringResource(R.string.battery_app),
                            selectedAppName = state.batteryAppName,
                            onDefaultClick = { viewModel.onBatteryAppChanged("") },
                            onChooseClick = { showBatteryAppPicker = true }
                        )
                    }

                    SettingsSpacer(4.dp)

                    AppSelectionItem(
                        title = stringResource(R.string.clock_app),
                        selectedAppName = state.clockAppName,
                        onDefaultClick = { viewModel.onClockAppChanged("") },
                        onChooseClick = { showClockAppPicker = true }
                    )

                    SettingsSpacer(4.dp)

                    AppSelectionItem(
                        title = stringResource(R.string.calendar_app),
                        selectedAppName = state.calendarAppName,
                        onDefaultClick = { viewModel.onCalendarAppChanged("") },
                        onChooseClick = { showCalendarAppPicker = true }
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Behavior) {
                ToggleItem(
                    title = stringResource(R.string.show_status_bar),
                    isChecked = state.showStatusBar,
                    onToggleClick = viewModel::onToggleShowStatusBar
                )
            }

            SettingsSpacer(4.dp)

            InSection(SettingsSection.Behavior) {
                ToggleItem(
                    title = stringResource(R.string.show_navigation_bar),
                    isChecked = state.showNavigationBar,
                    onToggleClick = viewModel::onToggleShowNavigationBar
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Search) {
                ToggleItem(
                    title = stringResource(R.string.show_keyboard),
                    subtitle = stringResource(R.string.show_keyboard_when_the_drawer_is_opened),
                    isChecked = state.autoOpenKeyboardAllApps,
                    onToggleClick = viewModel::onToggleAutoOpenKeyboardAllApps
                )
            }

            InSection(SettingsSection.Search) {
                if (isSearching || state.autoOpenKeyboardAllApps) {
                    SettingsSpacer(4.dp)

                    KeyboardDelayItem(
                        title = stringResource(R.string.keyboard_open_delay),
                        delayMs = state.keyboardOpenDelay,
                        onClick = {
                            keyboardOpenDelayInput = state.keyboardOpenDelay.toString()
                            showKeyboardOpenDelayDialog = true
                        }
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Behavior) {
                ToggleItem(
                    title = stringResource(R.string.double_tap_to_lock),
                    subtitle = stringResource(R.string.on_home_screen_double_tap_on_empty_space_to_lock),
                    // Show the effective state without erasing the saved user choice when the OS
                    // temporarily reports the lock-screen permission as unavailable.
                    isChecked = state.doubleTapToLock && hasLockScreenPermission,
                    onToggleClick = {
                        // Recheck on each tap because the permission may have changed outside the app.
                        val permissionGranted = context.hasLockScreenPermission()
                        hasLockScreenPermission = permissionGranted

                        when {
                            // The feature is active, so this tap explicitly disables the preference.
                            // On Android 8, removeLockScreenPermission also revokes device admin.
                            state.doubleTapToLock && permissionGranted -> {
                                viewModel.onDoubleTapToLockChanged(false)
                                context.removeLockScreenPermission()
                            }

                            // Permission already exists; no system settings round trip is required.
                            permissionGranted -> {
                                viewModel.onDoubleTapToLockChanged(true)
                            }

                            // Android 9+ requires the user to enable Minimo's accessibility service.
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                                showEnableAccessibilityDialog = true
                            }

                            // Android 8 uses the legacy device-admin permission flow directly.
                            else -> {
                                viewModel.onDoubleTapToLockChanged(true)
                                context.requestLockScreenPermission()
                            }
                        }
                    }
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Search) {
                ToggleItem(
                    title = stringResource(R.string.show_hidden_apps),
                    subtitle = stringResource(R.string.show_hidden_apps_in_the_search_result_of_the_app_drawer),
                    isChecked = state.showHiddenAppsInSearch,
                    onToggleClick = viewModel::onToggleShowHiddenAppsInSearch
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Search) {
                ToggleItem(
                    title = stringResource(R.string.auto_open_app),
                    subtitle = stringResource(R.string.automatically_open_the_app_if_it_is_the_only_search_result),
                    isChecked = state.autoOpenApp,
                    onToggleClick = viewModel::onToggleAutoOpenApp
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Home) {
                ToggleItem(
                    title = stringResource(R.string.notification_dots),
                    subtitle = stringResource(R.string.display_a_notification_dot_on_the_home_screen_apps),
                    isChecked = state.notificationDot,
                    onToggleClick = {
                        if (state.notificationDot) {
                            viewModel.onToggleNotificationDot()
                        } else {
                            if (context.isNotificationPermissionGranted()) {
                                viewModel.onToggleNotificationDot()
                            } else {
                                showEnableNotificationPermissionDialog = true
                            }
                        }
                    }
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Search) {
                IgnoreSpecialCharacters(
                    currentCharacters = state.ignoreSpecialCharacters,
                    onUpdateCharacters = viewModel::onUpdateIgnoreSpecialCharacters
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Search) {
                SearchBarBorderSlider(
                    searchBarBorderPercent = state.searchBarBorderPercent,
                    searchBarBackground = state.searchBarBackground,
                    onSearchBarBorderPercentChanged = viewModel::onSearchBarBorderPercentChanged
                )
            }

            SettingsSpacer(12.dp)

            InSection(SettingsSection.Search) {
                ToggleItem(
                    title = stringResource(R.string.search_bar_background),
                    isChecked = state.searchBarBackground,
                    onToggleClick = viewModel::onToggleSearchBarBackground
                )
            }

            InSection(SettingsSection.Search) {
                SearchModeDropdown(
                    selectedOption = StringUtils.searchModeText(context, state.searchMode),
                    options = SearchMode.entries.map { mode ->
                        mode to StringUtils.searchModeText(context, mode)
                    },
                    onOptionSelected = viewModel::onSearchModeChanged
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Drawer) {
                ToggleItem(
                    title = stringResource(R.string.sort_apps_by_usage),
                    subtitle = stringResource(R.string.sort_apps_by_usage_description),
                    isChecked = state.sortAppsByUsage,
                    onToggleClick = viewModel::onToggleSortAppsByUsage
                )
            }

            SettingsSpacer(4.dp)

            InSection(SettingsSection.Search) {
                ToggleItem(
                    title = stringResource(R.string.hide_app_drawer_search),
                    subtitle = stringResource(R.string.hide_app_drawer_search_description),
                    isChecked = state.hideAppDrawerSearch,
                    onToggleClick = viewModel::onToggleHideAppDrawerSearch
                )
            }

            InSection(SettingsSection.Search) {
                if (isSearching || state.hideAppDrawerSearch) {
                    MinimoSettingsPositionDropdown(
                        selectedOption = StringUtils.minimoSettingsPositionText(
                            context = context,
                            position = state.minimoSettingsPosition
                        ),
                        options = listOf(
                            MinimoSettingsPosition.Auto to StringUtils.minimoSettingsPositionText(
                                context,
                                MinimoSettingsPosition.Auto
                            ),
                            MinimoSettingsPosition.Top to StringUtils.minimoSettingsPositionText(
                                context,
                                MinimoSettingsPosition.Top
                            ),
                            MinimoSettingsPosition.Bottom to StringUtils.minimoSettingsPositionText(
                                context,
                                MinimoSettingsPosition.Bottom
                            )
                        ),
                        onOptionSelected = viewModel::onMinimoSettingsPositionChanged
                    )
                }
            }

            InSection(SettingsSection.Search) {
                if (isSearching || !state.hideAppDrawerSearch) {
                    SettingsSpacer(4.dp)

                    ToggleItem(
                        title = stringResource(R.string.search_bar_at_bottom),
                        subtitle = stringResource(R.string.change_the_position_of_the_app_drawer_search_bar_to_the_bottom),
                        isChecked = state.drawerSearchBarAtBottom,
                        onToggleClick = viewModel::onToggleDrawerSearchBarAtBottom
                    )
                }
            }

            InSection(SettingsSection.Widgets) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

                    ToggleItem(
                        title = stringResource(R.string.show_screen_time),
                        subtitle = stringResource(R.string.show_screen_time_description),
                        isChecked = state.showScreenTimeWidget,
                        onToggleClick = {
                            if (isSearching || state.showScreenTimeWidget) {
                                viewModel.onToggleShowScreenTimeWidget()
                            } else {
                                if (context.isAppUsagePermissionGranted()) {
                                    viewModel.onToggleShowScreenTimeWidget()
                                } else {
                                    showEnableAppUsagePermissionDialog = true
                                }
                            }
                        }
                    )

                    if (isSearching || state.showScreenTimeWidget) {
                        SettingsSpacer(4.dp)

                        AppSelectionItem(
                            title = stringResource(R.string.screen_time_app),
                            selectedAppName = state.screenTimeAppName,
                            onDefaultClick = { viewModel.onScreenTimeAppChanged("") },
                            onChooseClick = { showScreenTimeAppPicker = true }
                        )
                    }

                    SettingsSpacer(4.dp)

                    ToggleItem(
                        title = stringResource(R.string.show_app_screen_time),
                        subtitle = stringResource(R.string.show_app_screen_time_description),
                        isChecked = state.showAppScreenTime,
                        onToggleClick = {
                            if (state.showAppScreenTime || context.isAppUsagePermissionGranted()) {
                                viewModel.onToggleShowAppScreenTime()
                            } else {
                                showEnableAppUsagePermissionDialog = true
                            }
                        }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

                    if (LocalSettingsQuery.current.isBlank()) {
                        Text(
                            text = stringResource(R.string.time_limits_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING)
                        )
                    }

                    ToggleItem(
                        title = stringResource(R.string.limit_color_time_only),
                        subtitle = stringResource(R.string.limit_color_time_only_description),
                        isChecked = state.limitColorTimeOnly,
                        onToggleClick = viewModel::onToggleLimitColorTimeOnly
                    )

                    ColorPickerItem(
                        title = stringResource(R.string.limit_warning_color),
                        color = state.limitWarningColor
                            ?: TimeLimitRepository.DEFAULT_WARNING_COLOR,
                        onColorSelected = viewModel::onLimitWarningColorChanged
                    )

                    ColorPickerItem(
                        title = stringResource(R.string.limit_exceeded_color),
                        color = state.limitExceededColor
                            ?: TimeLimitRepository.DEFAULT_EXCEEDED_COLOR,
                        onColorSelected = viewModel::onLimitExceededColorChanged
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Widgets) {
                ToggleItem(
                    title = stringResource(R.string.show_home_note),
                    subtitle = stringResource(R.string.show_home_note_description),
                    isChecked = state.showHomeNote,
                    onToggleClick = viewModel::onToggleShowHomeNote
                )
            }

            SettingsSpacer(4.dp)

            InSection(SettingsSection.Widgets) {
                ToggleItem(
                    title = stringResource(R.string.show_weather),
                    subtitle = stringResource(R.string.show_weather_description),
                    isChecked = state.showWeather,
                    onToggleClick = viewModel::onToggleShowWeather
                )
            }

            InSection(SettingsSection.Widgets) {
                if (isSearching || state.showWeather) {
                    SettingsSpacer(4.dp)

                    WeatherCityItem(
                        currentCity = state.weatherCity,
                        onSearch = viewModel::onWeatherCitySearch,
                        onCitySelected = viewModel::onWeatherCitySelected,
                        onDetectLocation = viewModel::onDetectWeatherLocation
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Behavior) {
                AppSelectionItem(
                    title = stringResource(R.string.home_swipe_left_app),
                    selectedAppName = state.swipeLeftAppName,
                    defaultText = stringResource(R.string.none),
                    onDefaultClick = { viewModel.onSwipeLeftAppChanged("") },
                    onChooseClick = { showSwipeLeftAppPicker = true }
                )
            }

            SettingsSpacer(4.dp)

            InSection(SettingsSection.Behavior) {
                AppSelectionItem(
                    title = stringResource(R.string.home_swipe_right_app),
                    selectedAppName = state.swipeRightAppName,
                    defaultText = stringResource(R.string.none),
                    onDefaultClick = { viewModel.onSwipeRightAppChanged("") },
                    onChooseClick = { showSwipeRightAppPicker = true }
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Drawer) {
                ToggleItem(
                    title = stringResource(R.string.fast_scroller),
                    subtitle = stringResource(R.string.fast_scroller_description),
                    isChecked = state.enableFastScroller,
                    onToggleClick = viewModel::onToggleFastScroller
                )
            }

            InSection(SettingsSection.Drawer) {
                if (isSearching || state.enableFastScroller) {
                    FastScrollerAlignmentDropdown(
                        selectedOption = StringUtils.fastScrollerAlignmentText(
                            context,
                            state.fastScrollerAlignment
                        ),
                        options = listOf(
                            FastScrollerAlignment.Left to StringUtils.fastScrollerAlignmentText(
                                context,
                                FastScrollerAlignment.Left
                            ),
                            FastScrollerAlignment.Right to StringUtils.fastScrollerAlignmentText(
                                context,
                                FastScrollerAlignment.Right
                            )
                        ),
                        onOptionSelected = viewModel::onFastScrollerAlignmentChanged
                    )
                }
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Behavior) {
                ToggleItem(
                    title = stringResource(R.string.back_opens_drawer),
                    subtitle = stringResource(R.string.back_opens_drawer_description),
                    isChecked = state.backOpensAppDrawer,
                    onToggleClick = viewModel::onToggleBackOpensAppDrawer
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Drawer) {
                ToggleItem(
                    title = stringResource(R.string.hide_settings_icon),
                    subtitle = stringResource(R.string.hide_settings_icon_description),
                    isChecked = state.hideSettingsIcon,
                    onToggleClick = viewModel::onToggleHideSettingsIcon
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Home) {
                ToggleItem(
                    title = stringResource(R.string.compact_app_touch_area),
                    subtitle = stringResource(R.string.compact_app_touch_area_description),
                    isChecked = state.compactAppTouchArea,
                    onToggleClick = viewModel::onToggleCompactAppTouchArea
                )
            }

            SettingsDivider(modifier = Modifier.padding(vertical = 16.dp))

            InSection(SettingsSection.Search) {
                ToggleItem(
                    title = stringResource(R.string.done_opens_first_app),
                    subtitle = stringResource(R.string.done_opens_first_app_description),
                    isChecked = state.keyboardDoneOpensFirstApp,
                    onToggleClick = viewModel::onToggleKeyboardDoneOpensFirstApp
                )
            }

            SettingsSpacer(8.dp)
        }
        }

        if (showEnableAccessibilityDialog) {
            EnableAccessibilityDialog(
                onConfirm = {
                    // Persist the user's intent before leaving the app. The switch only appears
                    // enabled after the lifecycle permission check observes the granted service.
                    viewModel.onDoubleTapToLockChanged(true)
                    context.requestLockScreenPermission()
                    showEnableAccessibilityDialog = false
                },
                onDismiss = {
                    showEnableAccessibilityDialog = false
                }
            )
        }

        if (showEnableNotificationPermissionDialog) {
            EnableNotificationsDialog(
                onConfirm = {
                    context.openNotificationSettings()
                    showEnableNotificationPermissionDialog = false
                    viewModel.onToggleNotificationDot()
                },
                onDismiss = {
                    showEnableNotificationPermissionDialog = false
                }
            )
        }

        if (showEnableAppUsagePermissionDialog) {
            EnableAppUsageDialog(
                onConfirm = {
                    context.openUsageAccessSettings()
                    showEnableAppUsagePermissionDialog = false
                },
                onDismiss = {
                    showEnableAppUsagePermissionDialog = false
                }
            )
        }

        if (showSetWallpaperToThemeColorDialog) {
            EnableSetWallpaperToThemeColorDialog(
                onConfirm = {
                    viewModel.onToggleSetWallpaperToThemeColor()
                    showSetWallpaperToThemeColorDialog = false
                },
                onDismiss = {
                    showSetWallpaperToThemeColorDialog = false
                }
            )
        }

        if (showKeyboardOpenDelayDialog) {
            KeyboardDelayDialog(
                title = stringResource(R.string.keyboard_open_delay),
                currentDelay = keyboardOpenDelayInput,
                delayRange = KEYBOARD_OPEN_DELAY_RANGE,
                defaultDelay = Constants.DEFAULT_KEYBOARD_OPEN_DELAY,
                onDelayChange = { keyboardOpenDelayInput = it },
                onUpdate = { delay ->
                    viewModel.onKeyboardOpenDelayChanged(delay)
                    showKeyboardOpenDelayDialog = false
                },
                onDismiss = {
                    showKeyboardOpenDelayDialog = false
                }
            )
        }


        if (showClockAppPicker) {
            AppPickerDialog(
                onDismissRequest = { showClockAppPicker = false },
                onAppSelected = { appInfo ->
                    viewModel.onClockAppChanged(appInfo.preferenceValue)
                    showClockAppPicker = false
                }
            )
        }

        if (showBatteryAppPicker) {
            AppPickerDialog(
                onDismissRequest = { showBatteryAppPicker = false },
                onAppSelected = { appInfo ->
                    viewModel.onBatteryAppChanged(appInfo.preferenceValue)
                    showBatteryAppPicker = false
                }
            )
        }

        if (showCalendarAppPicker) {
            AppPickerDialog(
                onDismissRequest = { showCalendarAppPicker = false },
                onAppSelected = { appInfo ->
                    viewModel.onCalendarAppChanged(appInfo.preferenceValue)
                    showCalendarAppPicker = false
                }
            )
        }

        if (showScreenTimeAppPicker) {
            AppPickerDialog(
                onDismissRequest = { showScreenTimeAppPicker = false },
                onAppSelected = { appInfo ->
                    viewModel.onScreenTimeAppChanged(appInfo.preferenceValue)
                    showScreenTimeAppPicker = false
                }
            )
        }

        if (showSwipeLeftAppPicker) {
            AppPickerDialog(
                onDismissRequest = { showSwipeLeftAppPicker = false },
                onAppSelected = { appInfo ->
                    viewModel.onSwipeLeftAppChanged(appInfo.preferenceValue)
                    showSwipeLeftAppPicker = false
                }
            )
        }

        if (showSwipeRightAppPicker) {
            AppPickerDialog(
                onDismissRequest = { showSwipeRightAppPicker = false },
                onAppSelected = { appInfo ->
                    viewModel.onSwipeRightAppChanged(appInfo.preferenceValue)
                    showSwipeRightAppPicker = false
                }
            )
        }
    }

    if (embedded) {
        content(PaddingValues())
    } else {
    Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                        .padding(horizontal = 4.dp)
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = section?.let { stringResource(it.titleRes()) }
                            ?: stringResource(R.string.customisation),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 64.dp)
                    )
    
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues -> content(paddingValues) }
    }
}

@Composable
fun AppSelectionItem(
    title: String,
    selectedAppName: String,
    defaultText: String = stringResource(R.string.default_app),
    onDefaultClick: () -> Unit,
    onChooseClick: () -> Unit
) {
    if (!settingVisible(title, selectedAppName)) return

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .padding(
                horizontal = Dimens.APP_HORIZONTAL_SPACING,
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(0.65f),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier.weight(0.35f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box {
                Text(
                    text = selectedAppName.ifEmpty { defaultText },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(defaultText) },
                        onClick = {
                            showMenu = false
                            onDefaultClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.choose)) },
                        onClick = {
                            showMenu = false
                            onChooseClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardDelayItem(
    title: String,
    delayMs: Long,
    onClick: () -> Unit
) {
    if (!settingVisible(title)) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = Dimens.APP_HORIZONTAL_SPACING,
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(0.65f),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier.weight(0.35f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "${delayMs}ms",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun KeyboardDelayDialog(
    title: String,
    currentDelay: String,
    delayRange: ClosedRange<Long>,
    defaultDelay: Long,
    onDelayChange: (String) -> Unit,
    onUpdate: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    var delayValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = currentDelay,
                selection = TextRange(currentDelay.length)
            )
        )
    }

    val isValid = delayValue.text.toLongOrNull()?.let { it in delayRange } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = delayValue,
                    onValueChange = { newValue ->
                        if (newValue.text.all { it.isDigit() }) {
                            delayValue = newValue
                            onDelayChange(newValue.text)
                        }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.delay_in_milliseconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.keyboard_delay_min, delayRange.start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.keyboard_delay_max, delayRange.endInclusive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.keyboard_delay_default, defaultDelay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    delayValue.text.toLongOrNull()?.let { onUpdate(it) }
                },
                enabled = isValid
            ) {
                Text(text = stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.dismiss))
            }
        }
    )

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}

@androidx.annotation.StringRes
fun SettingsSection.titleRes(): Int = when (this) {
    SettingsSection.Look -> R.string.section_look
    SettingsSection.Home -> R.string.section_home
    SettingsSection.Widgets -> R.string.section_widgets
    SettingsSection.Drawer -> R.string.section_drawer
    SettingsSection.Search -> R.string.section_search
    SettingsSection.Behavior -> R.string.section_behavior
}
