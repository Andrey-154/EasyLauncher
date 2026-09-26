package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.minimo.launcher.ui.entities.AppInfo
import kotlin.math.abs

/** How many times the list is repeated in each direction; far more than anyone scrolls. */
private const val LOOPS = 2_000

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
fun HomeAppsCarousel(
    apps: List<AppInfo>,
    visibleCount: Int,
    transformOriginX: Float,
    modifier: Modifier = Modifier,
    itemContent: @Composable (AppInfo, Modifier) -> Unit
) {
    val size = apps.size
    // Start in the middle, on the first app, so both directions have room
    val listState = remember(size) { LazyListState(firstVisibleItemIndex = size * (LOOPS / 2)) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val scaleEdges = visibleCount > 4

    val heightModifier = if (rowHeightPx > 0) {
        Modifier.height(with(LocalDensity.current) { (rowHeightPx * visibleCount).toDp() })
    } else {
        Modifier
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier),
        // Settle with a row exactly at the top: the window shows only whole rows
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start)
    ) {
        items(count = size * LOOPS) { index ->
            itemContent(
                apps[index % size],
                Modifier
                    .onSizeChanged { if (it.height > 0) rowHeightPx = it.height }
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
