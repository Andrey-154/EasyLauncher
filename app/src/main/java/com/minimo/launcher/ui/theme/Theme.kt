package com.minimo.launcher.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.set
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minimo.launcher.utils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DarkColorScheme = darkColorScheme()

private val LightColorScheme = lightColorScheme()

private val BlackColorScheme = darkColorScheme(
    onSurface = Color.White,
    surface = Color.Black
)

/** User's own text color, or [Color.Unspecified]. Also used over the wallpaper. */
val LocalCustomTextColor = staticCompositionLocalOf { Color.Unspecified }

/** Replaces theme colors with the user's own background / text / accent colors. */
private fun ColorScheme.withCustomColors(
    background: Color?,
    text: Color?,
    accent: Color?
): ColorScheme {
    var scheme = this
    if (background != null) {
        // Containers (dialogs, menus, sheets) are the background shifted a bit to the text color
        val towards = if (background.luminance() > 0.5f) Color.Black else Color.White
        scheme = scheme.copy(
            surface = background,
            background = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = lerp(background, towards, 0.04f),
            surfaceContainer = lerp(background, towards, 0.07f),
            surfaceContainerHigh = lerp(background, towards, 0.10f),
            surfaceContainerHighest = lerp(background, towards, 0.14f),
            surfaceVariant = lerp(background, towards, 0.14f),
            inverseSurface = towards,
            inverseOnSurface = background
        )
    }
    if (text != null) {
        scheme = scheme.copy(
            onSurface = text,
            onBackground = text,
            onSurfaceVariant = text.copy(alpha = 0.75f).compositeOver(scheme.surface),
            outline = text.copy(alpha = 0.5f).compositeOver(scheme.surface),
            outlineVariant = text.copy(alpha = 0.25f).compositeOver(scheme.surface)
        )
    }
    if (accent != null) {
        val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
        val container = accent.copy(alpha = 0.3f).compositeOver(scheme.surface)
        scheme = scheme.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = container,
            onPrimaryContainer = scheme.onSurface,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = container,
            onSecondaryContainer = scheme.onSurface,
            tertiary = accent,
            onTertiary = onAccent,
            inversePrimary = accent,
            surfaceTint = accent
        )
    }
    return scheme
}

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    blackTheme: Boolean,
    useDynamicTheme: Boolean,
    statusBarVisible: Boolean,
    navigationBarVisible: Boolean,
    setWallpaperToThemeColor: Boolean,
    enableWallpaper: Boolean,
    isHomeScreen: Boolean,
    lightTextOnWallpaper: Boolean,
    fontPreference: String = "",
    customBackgroundColor: Int? = null,
    customTextColor: Int? = null,
    customAccentColor: Int? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDynamicTheme = useDynamicTheme && AndroidUtils.isDynamicThemeSupported()
    var isLightTheme = false

    fun getDarkTheme(context: Context): ColorScheme {
        return if (isDynamicTheme) {
            if (blackTheme) {
                dynamicDarkColorScheme(context).copy(
                    onSurface = Color.White,
                    surface = Color.Black
                )
            } else {
                dynamicDarkColorScheme(context)
            }
        } else {
            if (blackTheme) {
                BlackColorScheme
            } else {
                DarkColorScheme
            }
        }
    }

    fun getLightTheme(context: Context): ColorScheme {
        return if (isDynamicTheme) {
            dynamicLightColorScheme(context)
        } else {
            LightColorScheme
        }
    }

    val baseColorScheme = when (themeMode) {
        ThemeMode.System -> if (isSystemInDarkTheme()) {
            getDarkTheme(context)
        } else {
            isLightTheme = true
            getLightTheme(context)
        }

        ThemeMode.Dark -> getDarkTheme(context)

        ThemeMode.Light -> {
            isLightTheme = true
            getLightTheme(context)
        }
    }

    val colorScheme = baseColorScheme.withCustomColors(
        background = customBackgroundColor?.let { Color(it) },
        text = customTextColor?.let { Color(it) },
        accent = customAccentColor?.let { Color(it) }
    )
    if (customBackgroundColor != null) {
        // System bar icons must contrast with the custom background
        isLightTheme = Color(customBackgroundColor).luminance() > 0.5f
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val surfaceColor =
                if (enableWallpaper) Color.Transparent.toArgb() else colorScheme.surface.toArgb()
            window.statusBarColor = surfaceColor
            window.navigationBarColor = surfaceColor

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            val insetsController = WindowCompat.getInsetsController(window, view)

            if (enableWallpaper) {
                insetsController.isAppearanceLightStatusBars = !lightTextOnWallpaper
                if (!isHomeScreen) {
                    insetsController.isAppearanceLightNavigationBars = !lightTextOnWallpaper
                }
                // HomeScreen owns its navigation icons because the app drawer can cover that area.
            } else {
                insetsController.isAppearanceLightStatusBars = isLightTheme
                insetsController.isAppearanceLightNavigationBars = isLightTheme
            }

            if (!statusBarVisible || !navigationBarVisible) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            if (statusBarVisible) {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }

            if (navigationBarVisible) {
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }

            window.setBackgroundDrawable(surfaceColor.toDrawable())
        }
    }

    if (setWallpaperToThemeColor) {
        // Only run when the background color changes, and execute setting the wallpaper on the IO thread.
        LaunchedEffect(colorScheme.surface) {
            withContext(Dispatchers.IO) {
                updateWallpaper(context, colorScheme.surface)
            }
        }
    }

    CompositionLocalProvider(
        LocalCustomTextColor provides (customTextColor?.let { Color(it) } ?: Color.Unspecified)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypographyForFont(fontPreference),
            content = content
        )
    }
}

private fun updateWallpaper(context: Context, color: Color) {
    try {
        val wallpaperManager = WallpaperManager.getInstance(context)

        val bitmap = createBitmap(1, 1)
        bitmap[0, 0] = color.toArgb()

        wallpaperManager.setBitmap(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
