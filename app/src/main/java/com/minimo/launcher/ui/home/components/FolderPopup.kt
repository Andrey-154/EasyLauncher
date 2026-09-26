package com.minimo.launcher.ui.home.components

import com.minimo.launcher.ui.components.BlurBehind
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.minimo.launcher.R
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.utils.FolderOpenStyle
import com.minimo.launcher.utils.HomeButton
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

private const val MAX_VISIBLE_ROWS = 3
private val PANEL_SHAPE = RoundedCornerShape(32.dp)
private val CELL_WIDTH = 84.dp
private val GRID_ICON = 56.dp
private val LIST_ICON = 36.dp
private val NAME_HEIGHT = 24.dp
private val CELL_PADDING = 8.dp

/** Remembers where this element is on the screen (its centre), e.g. to grow a folder from it. */
@Composable
fun Modifier.onScreenCenter(onCenter: (Offset) -> Unit): Modifier {
    val view = LocalView.current
    return onGloballyPositioned { coords ->
        val window = IntArray(2)
        view.rootView.getLocationOnScreen(window)
        val position = coords.positionInWindow()
        onCenter(
            Offset(
                position.x + window[0] + coords.size.width / 2f,
                position.y + window[1] + coords.size.height / 2f
            )
        )
    }
}

/**
 * An open folder. It grows out of the tapped folder ([origin], screen coordinates) with a soft
 * spring, and shrinks back into it when closed. Grid style is just big enough for its apps
 * (up to 12 visible, the rest scroll); list style shows names next to icons.
 */
@Composable
fun FolderPopup(
    folder: HomeButton,
    origin: Offset?,
    findApp: (String) -> AppInfo?,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    BlurBehind()
    val apps = remember(folder) { folder.apps.mapNotNull(findApp) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    fun close(then: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f, tween(durationMillis = 170, easing = FastOutLinearInEasing))
            then()
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 420f))
    }

    Dialog(
        onDismissRequest = { close(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        // Our own animated dim instead of the system's instant grey
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }

        val view = LocalView.current
        var panelOrigin by remember { mutableStateOf(TransformOrigin.Center) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.35f * progress.value.coerceIn(0f, 1f)))
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { close(onDismiss) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        if (origin == null || coords.size.width == 0) return@onGloballyPositioned
                        val window = IntArray(2)
                        view.rootView.getLocationOnScreen(window)
                        val topLeft = coords.positionInWindow()
                        panelOrigin = TransformOrigin(
                            ((origin.x - topLeft.x - window[0]) / coords.size.width).coerceIn(0f, 1f),
                            ((origin.y - topLeft.y - window[1]) / coords.size.height).coerceIn(0f, 1f)
                        )
                    }
                    .graphicsLayer {
                        val p = progress.value
                        val scale = 0.2f + 0.8f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = p.coerceIn(0f, 1f)
                        transformOrigin = panelOrigin
                    }
                    .shadow(24.dp, PANEL_SHAPE)
                    .clip(PANEL_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), PANEL_SHAPE)
                    // Taps inside the folder must not close it
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = folder.name.ifEmpty { stringResource(R.string.folder) },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = CELL_WIDTH * 4)
                )
                Spacer(modifier = Modifier.height(14.dp))

                val launch: (AppInfo) -> Unit = { app -> close { onAppClick(app) } }
                when {
                    apps.isEmpty() -> Text(
                        text = stringResource(R.string.folder_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    folder.openStyle == FolderOpenStyle.Grid ->
                        FolderGrid(apps, folder.showNames, loadAppIcon, launch)

                    else -> FolderList(apps, loadAppIcon, launch)
                }
            }
        }
    }
}

@Composable
private fun FolderGrid(
    apps: List<AppInfo>,
    showNames: Boolean,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit
) {
    // Just big enough: 1–2 in a row, 2×2, 3×3, then 4 columns
    val columns = when {
        apps.size <= 2 -> apps.size
        apps.size <= 4 -> 2
        apps.size <= 9 -> 3
        else -> 4
    }
    val rows = ceil(apps.size / columns.toFloat()).toInt()
    val cellHeight = GRID_ICON + CELL_PADDING * 2 + if (showNames) NAME_HEIGHT else 0.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .width(CELL_WIDTH * columns)
            .height(cellHeight * min(rows, MAX_VISIBLE_ROWS))
    ) {
        items(apps, key = { it.id }) { app ->
            PressableColumn(onClick = { onAppClick(app) }) {
                AppIconImage(app, GRID_ICON, loadAppIcon)
                if (showNames) {
                    // As much of the name as fits the cell
                    Text(
                        text = app.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(CELL_WIDTH - 8.dp)
                            .height(NAME_HEIGHT)
                            .padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderList(
    apps: List<AppInfo>,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 240.dp, max = 320.dp)
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        apps.forEach { app ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "rowPress")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        else Color.Transparent
                    )
                    .clickable(interaction, indication = null) { onAppClick(app) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(app, LIST_ICON, loadAppIcon)
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = app.name,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A cell that gently sinks while pressed, instead of a square ripple. */
@Composable
private fun PressableColumn(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "cellPress")
    Column(
        modifier = Modifier
            .width(CELL_WIDTH)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(vertical = CELL_PADDING, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
private fun AppIconImage(app: AppInfo, size: Dp, loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, app.id, sizePx) { value = loadAppIcon(app, sizePx) }
    // Same look as the home list: icon bitmaps are square, AppIcon clips them round
    AppIcon(image = icon, size = size, isWorkProfile = app.isWorkProfile, showNotificationDot = false)
}
