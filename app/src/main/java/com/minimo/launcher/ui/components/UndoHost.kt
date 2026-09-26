package com.minimo.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R
import com.minimo.launcher.utils.UndoController
import com.minimo.launcher.utils.UndoRequest
import kotlinx.coroutines.launch

private const val UNDO_MILLIS = 5_000

/** A small bar at the bottom: "Removed · Undo", with a line that runs out in 5 seconds. */
@Composable
fun UndoHost(controller: UndoController, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<UndoRequest?>(null) }
    var visible by remember { mutableStateOf(false) }
    val timeLeft = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(controller) {
        controller.requests.collect { request ->
            current = request
            visible = true
            timeLeft.snapTo(1f)
            // A newer request restarts the timer (collect is cancelled-and-replaced by the next item)
            scope.launch {
                timeLeft.animateTo(0f, tween(UNDO_MILLIS, easing = LinearEasing))
                if (current === request) visible = false
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            val request = current ?: return@AnimatedVisibility
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(start = 18.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = request.message,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            visible = false
                            scope.launch { request.undo() }
                        }) {
                            Text(
                                text = stringResource(R.string.undo),
                                color = MaterialTheme.colorScheme.inversePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Time left
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(timeLeft.value)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}
