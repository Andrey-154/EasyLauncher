package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.HomeActionButton
import com.minimo.launcher.ui.components.icon
import com.minimo.launcher.ui.components.title
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.utils.HomeButton
import com.minimo.launcher.utils.HomeButtonSize
import com.minimo.launcher.utils.HomeButtonStyle
import com.minimo.launcher.utils.HomeButtonType
import kotlin.math.roundToInt

/**
 * Free-placed round buttons over the home screen. Long-press any button to enter edit mode,
 * then drag buttons anywhere, remove them with ✕ and press "Done".
 */
@Composable
fun HomeButtonsLayer(
    buttons: List<HomeButton>,
    buttonSize: HomeButtonSize,
    buttonStyle: HomeButtonStyle,
    editMode: Boolean,
    contentColor: Color,
    flashlightOn: Boolean,
    findApp: (String) -> AppInfo?,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onClick: (HomeButton) -> Unit,
    onEnterEditMode: () -> Unit,
    onMoved: (HomeButton, Float, Float) -> Unit,
    onRemove: (HomeButton) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (buttons.isEmpty() && !editMode) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val sizeDp = buttonSize.dp.dp
        val sizePx = with(density) { sizeDp.toPx() }
        val freeWidth = (constraints.maxWidth - sizePx).coerceAtLeast(1f)
        val freeHeight = (constraints.maxHeight - sizePx).coerceAtLeast(1f)

        buttons.forEach { button ->
            val app = if (button.type == HomeButtonType.APP) findApp(button.app) else null
            val appIcon by produceState<ImageBitmap?>(null, app?.id, sizePx) {
                value = app?.let { loadAppIcon(it, sizePx.roundToInt()) }
            }

            var position by remember(button.id, button.x, button.y, freeWidth, freeHeight) {
                mutableStateOf(Offset(button.x * freeWidth, button.y * freeHeight))
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                    .pointerInput(editMode, button.id, freeWidth, freeHeight) {
                        if (!editMode) return@pointerInput
                        detectDragGestures(
                            onDragEnd = {
                                onMoved(button, position.x / freeWidth, position.y / freeHeight)
                            }
                        ) { change, drag ->
                            change.consume()
                            position = Offset(
                                (position.x + drag.x).coerceIn(0f, freeWidth),
                                (position.y + drag.y).coerceIn(0f, freeHeight)
                            )
                        }
                    }
            ) {
                HomeActionButton(
                    icon = button.type.icon(),
                    contentDescription = app?.name ?: stringResource(button.type.title()),
                    contentColor = contentColor,
                    size = sizeDp,
                    style = buttonStyle,
                    active = button.type == HomeButtonType.FLASHLIGHT && flashlightOn,
                    appIcon = appIcon,
                    onClick = { if (!editMode) onClick(button) },
                    onLongClick = { if (!editMode) onEnterEditMode() }
                )

                if (editMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .clickable { onRemove(button) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = MaterialTheme.colorScheme.onError, fontSize = 11.sp)
                    }
                }
            }
        }

        if (editMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (buttons.isEmpty()) R.string.home_buttons_edit_empty
                            else R.string.home_buttons_edit_hint
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}
