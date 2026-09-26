package com.minimo.launcher.ui.home.components

import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import android.os.SystemClock
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

/** How many times the list is repeated in each direction; far more than anyone scrolls. */
private const val LOOPS = 2_000

private const val MIN_TICK_INTERVAL_MS = 35L

/** Size of the outermost rows (at the very edge) when more than 4 apps are shown. */
private const val EDGE_SCALE = 0.8f
private const val EDGE_ALPHA = 0.55f

/**
 * Endless loop of favourites showing exactly [visibleCount] whole rows: after the last app the
 * first one comes again, and a fling always settles on a row boundary. With more than 4 rows the
 * outermost rows are a bit smaller and grow while they scroll towards the centre.
 *
 * @param transformOriginX 0 = rows scale towards the left edge, 0.5 = centre, 1 = right edge
 *  (matches the horizontal alignment of the apps).
 */
@Composable
fun <T> HomeAppsCarousel(
    apps: List<T>,
    visibleCount: Int,
    /** Every row gets exactly this height, so the window never changes size while scrolling. */
    rowHeight: Dp,
    transformOriginX: Float,
    modifier: Modifier = Modifier,
    /** Width of the scrollable strip ("compact touch area"); null = full width. */
    width: Dp? = null,
    /** Called for every row that passes while scrolling (tick sound / vibration). */
    onRowPassed: () -> Unit = {},
    /** Back to the first app: instantly when leaving Home, smoothly on these events ("Home"). */
    resetToStart: Boolean = false,
    resetEvents: Flow<Unit>? = null,
    itemContent: @Composable (T, Modifier) -> Unit
) {
    val size = apps.size
    // Start in the middle, on the first app, so both directions have room
    val listState = remember(size) { LazyListState(firstVisibleItemIndex = size * (LOOPS / 2)) }
    val scaleEdges = visibleCount > 4

    // Nearest row that shows the first app again (the list repeats every `size` rows)
    fun nearestStart(): Int {
        val current = listState.firstVisibleItemIndex
        val below = current - current % size
        return if (current - below <= size / 2) below else below + size
    }

    val scope = rememberCoroutineScope()
    if (resetToStart) {
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            scope.launch { listState.scrollToItem(nearestStart()) }
        }
        LaunchedEffect(resetEvents) {
            resetEvents?.collect { listState.animateScrollToItem(nearestStart()) }
        }
    }

    // A "wheel" tick for each row that crosses the top edge; limited in rate for fast flings
    val currentOnRowPassed by rememberUpdatedState(onRowPassed)
    LaunchedEffect(listState) {
        var lastTick = 0L
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1)
            .collect {
                val now = SystemClock.uptimeMillis()
                if (now - lastTick < MIN_TICK_INTERVAL_MS) return@collect
                lastTick = now
                currentOnRowPassed()
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(rowHeight * visibleCount),
        // Settle with a row exactly at the top: the window shows only whole rows
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start)
    ) {
        items(count = size * LOOPS) { index ->
            itemContent(
                apps[index % size],
                Modifier
                    .height(rowHeight)
                    .graphicsLayer {
                        if (!scaleEdges) return@graphicsLayer
                        val info = listState.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            ?: return@graphicsLayer
                        val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
                        if (half <= 0f) return@graphicsLayer
                        val viewportCenter = info.viewportStartOffset + half
                        val distance =
                            (abs(item.offset + item.size / 2f - viewportCenter) / half).coerceIn(0f, 1f)
                        // Rows up to the centre of the 2nd row from the edge keep full size;
                        // only the outermost row (on each side) shrinks
                        val fullSizeUntil = (visibleCount - 3f) / visibleCount
                        val edge = ((distance - fullSizeUntil) / (1f - fullSizeUntil)).coerceIn(0f, 1f)
                        val scale = 1f - (1f - EDGE_SCALE) * edge
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (1f - EDGE_ALPHA) * edge
                        transformOrigin = TransformOrigin(transformOriginX, 0.5f)
                    }
            )
        }
    }
}
