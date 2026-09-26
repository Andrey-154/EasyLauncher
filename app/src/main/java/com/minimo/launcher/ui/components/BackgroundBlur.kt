package com.minimo.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Counts the open overlays (folders, menus, dialogs); the launcher is blurred while any is open. */
class BlurController {
    var openOverlays by mutableIntStateOf(0)
        internal set
}

val LocalBlurController = staticCompositionLocalOf { BlurController() }

/** Call inside a dialog / sheet: the launcher behind it is blurred while it is shown. */
@Composable
fun BlurBehind() {
    val controller = LocalBlurController.current
    DisposableEffect(controller) {
        controller.openOverlays++
        onDispose { controller.openOverlays-- }
    }
}
