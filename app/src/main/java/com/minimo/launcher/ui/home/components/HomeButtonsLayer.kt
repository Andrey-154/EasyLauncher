package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * then drag buttons anywhere and remove them with ✕. Tap empty space or press Back to finish.
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

        if (editMode) {
            // Below the buttons: tapping empty space finishes editing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDone
                    )
            )
        }

        buttons.forEach { button ->
            val app = if (button.type == HomeButtonType.APP) findApp(button.app) else null
            val appIcon by produceState<ImageBitmap?>(null, app?.id, sizePx) {
                value = app?.let { loadAppIcon(it, sizePx.roundToInt()) }
            }

            // One state object per button for its whole life: the drag handler (started once)
            // must keep writing to the same object the offset reads from
            val position = remember(button.id) {
                mutableStateOf(Offset(button.x * freeWidth, button.y * freeHeight))
            }
            val dragging = remember(button.id) { mutableStateOf(false) }

            // Follow the saved position and screen size changes, but never fight an active drag
            LaunchedEffect(button.x, button.y, freeWidth, freeHeight) {
                if (!dragging.value) {
                    position.value = Offset(button.x * freeWidth, button.y * freeHeight)
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt())
                    }
                    .pointerInput(editMode, button.id, freeWidth, freeHeight) {
                        if (!editMode) return@pointerInput
                        fun save() {
                            dragging.value = false
                            onMoved(
                                button,
                                position.value.x / freeWidth,
                                position.value.y / freeHeight
                            )
                        }
                        detectDragGestures(
                            onDragStart = { dragging.value = true },
                            onDragEnd = { save() },
                            onDragCancel = { save() }
                        ) { change, drag ->
                            change.consume()
                            position.value = Offset(
                                (position.value.x + drag.x).coerceIn(0f, freeWidth),
                                (position.value.y + drag.y).coerceIn(0f, freeHeight)
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

                if (button.type == HomeButtonType.FOLDER && button.name.isNotEmpty()) {
                    Text(
                        text = button.name,
                        color = contentColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 18.dp)
                            .wrapContentWidth(unbounded = true)
                    )
                }

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

    }
}
