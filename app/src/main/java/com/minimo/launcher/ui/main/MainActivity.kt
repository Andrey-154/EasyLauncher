package com.minimo.launcher.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minimo.launcher.ui.home.HomeViewModel
import com.minimo.launcher.ui.navigation.AppNavGraph
import com.minimo.launcher.ui.navigation.DrawerCloseState
import com.minimo.launcher.ui.navigation.Routes
import com.minimo.launcher.ui.theme.AppTheme
import com.minimo.launcher.utils.AppsManager
import com.minimo.launcher.utils.HomePressedNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appsManager: AppsManager

    private val viewModel: MainViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setupOrientationChangeListener()
        appsManager.registerCallback()

        setContent {
            val navController = rememberNavController()
            val state by viewModel.state.collectAsState()

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val isHomeScreen = currentRoute == Routes.HOME
            val enableWallpaperOnCurrentScreen = when (currentRoute) {
                Routes.HOME -> state.enableWallpaper
                Routes.APP_DRAWER -> state.enableWallpaperOnDrawer
                else -> false
            }

            ObserveNavigationEvents(
                navController = navController,
                homePressedNotifier = viewModel.getHomePressedNotifier()
            )

            // An app was opened from the drawer: go back to Home right away (no animation),
            // so returning to the launcher does not replay the drawer -> home transition.
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                if (navController.currentDestination?.route == Routes.APP_DRAWER) {
                    DrawerCloseState.closedInBackground = true
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            }

            // Let the system draw the wallpaper behind the launcher only when it is used.
            // Otherwise Android runs the heavier "wallpaper" transitions when returning home.
            val wallpaperUsed = state.enableWallpaper || state.enableWallpaperOnDrawer
            SideEffect {
                if (wallpaperUsed) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                }
            }

            AppTheme(
                themeMode = state.themeMode,
                statusBarVisible = state.statusBarVisible,
                navigationBarVisible = state.navigationBarVisible,
                useDynamicTheme = state.useDynamicTheme,
                blackTheme = state.blackTheme,
                setWallpaperToThemeColor = state.setWallpaperToThemeColor,
                enableWallpaper = enableWallpaperOnCurrentScreen,
                isHomeScreen = isHomeScreen,
                lightTextOnWallpaper = state.lightTextOnWallpaper,
                fontPreference = state.fontPreference,
                customBackgroundColor = state.customBackgroundColor,
                customTextColor = state.customTextColor,
                customAccentColor = state.customAccentColor
            ) {
                val backgroundColor = when {
                    !enableWallpaperOnCurrentScreen -> MaterialTheme.colorScheme.surface
                    state.dimWallpaper -> Color.Black.copy(
                        alpha = state.dimWallpaperPercentage / 100f
                    )

                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    AppNavGraph(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        enableWallpaper = state.enableWallpaper,
                        statusBarVisible = state.statusBarVisible,
                        navigationBarVisible = state.navigationBarVisible,
                        onBackPressed = {
                            onBackPressedDispatcher.onBackPressed()
                        }
                    )
                }
            }
        }
    }

    private fun setupOrientationChangeListener() {
        lifecycleScope.launch {
            viewModel.state
                .map { it.screenOrientation.orientation }
                .distinctUntilChanged()
                .collect { orientation ->
                    requestedOrientation = orientation
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomeButtonPressed()
        }
    }

    override fun onDestroy() {
        appsManager.unregisterCallback()
        super.onDestroy()
    }
}

@Composable
private fun ObserveNavigationEvents(
    navController: NavHostController,
    homePressedNotifier: HomePressedNotifier
) {
    LaunchedEffect(
        key1 = navController,
        key2 = homePressedNotifier
    ) {
        homePressedNotifier.homePressedEvent.collect {
            if (navController.currentDestination?.route != Routes.HOME) {
                navController.popBackStack(Routes.HOME, inclusive = false)
            }
        }
    }
}
