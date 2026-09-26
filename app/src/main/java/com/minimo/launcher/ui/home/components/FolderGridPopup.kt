package com.minimo.launcher.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minimo.launcher.R
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.utils.HomeButton
import kotlin.math.ceil
import kotlin.math.min

private const val MAX_VISIBLE_ROWS = 3
private val CELL_WIDTH = 80.dp
private val ICON_SIZE = 48.dp
private val NAME_HEIGHT = 20.dp
private val CELL_PADDING = 8.dp

/**
 * A folder that "grows" out of the screen centre and is just big enough for its apps:
 * 1–2 apps in one row, up to 4 in 2×2, up to 9 in 3×3, then 4 columns. At most 12 apps
 * (3 rows) are visible at once; more are reached by scrolling.
 */
@Composable
fun FolderGridPopup(
    folder: HomeButton,
    findApp: (String) -> AppInfo?,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val apps = folder.apps.mapNotNull(findApp)
    val columns = when {
        apps.size <= 2 -> apps.size.coerceAtLeast(1)
        apps.size <= 4 -> 2
        apps.size <= 9 -> 3
        else -> 4
    }
    val rows = ceil(apps.size / columns.toFloat()).toInt().coerceAtLeast(1)
    val cellHeight = ICON_SIZE + CELL_PADDING * 2 + if (folder.showNames) NAME_HEIGHT else 0.dp

    val appear = remember { MutableTransitionState(false).apply { targetState = true } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Tap outside the folder closes it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = appear,
                enter = scaleIn(
                    initialScale = 0.4f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeIn()
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    // Taps inside the folder must not close it
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = folder.name.ifEmpty { stringResource(R.string.folder) },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(CELL_WIDTH * columns)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (apps.isEmpty()) {
                            Text(stringResource(R.string.folder_empty))
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier
                                    .width(CELL_WIDTH * columns)
                                    .height(cellHeight * min(rows, MAX_VISIBLE_ROWS))
                            ) {
                                items(apps, key = { it.id }) { app ->
                                    FolderCell(
                                        app = app,
                                        showName = folder.showNames,
                                        loadAppIcon = loadAppIcon,
                                        onClick = { onAppClick(app) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCell(
    app: AppInfo,
    showName: Boolean,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onClick: () -> Unit
) {
    val sizePx = with(LocalDensity.current) { ICON_SIZE.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, app.id) { value = loadAppIcon(app, sizePx) }

    Column(
        modifier = Modifier
            .width(CELL_WIDTH)
            .clickable(onClick = onClick)
            .padding(vertical = CELL_PADDING, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(ICON_SIZE), contentAlignment = Alignment.Center) {
            icon?.let { Image(bitmap = it, contentDescription = app.name, modifier = Modifier.size(ICON_SIZE)) }
        }
        if (showName) {
            // As much of the name as fits the cell
            Text(
                text = app.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(CELL_WIDTH - 8.dp)
                    .height(NAME_HEIGHT)
                    .padding(top = 4.dp)
            )
        }
    }
}
