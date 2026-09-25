package com.minimo.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R

private const val ANIMATION_MILLIS = 250

/**
 * Small round flashlight button. Off: thin outline with the icon. On: filled white with a
 * soft glow around it, like a lit lamp.
 */
@Composable
fun FlashlightButton(
    isOn: Boolean,
    onClick: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val glow by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = tween(ANIMATION_MILLIS),
        label = "flashlightGlow"
    )
    val background by animateColorAsState(
        targetValue = if (isOn) Color.White else Color.Transparent,
        animationSpec = tween(ANIMATION_MILLIS),
        label = "flashlightBackground"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF1C1C1C) else contentColor.copy(alpha = 0.8f),
        animationSpec = tween(ANIMATION_MILLIS),
        label = "flashlightIcon"
    )
    val borderColor = if (isOn) Color.White else contentColor.copy(alpha = 0.5f)

    Box(
        // Room for the glow so it is not clipped by neighbours
        modifier = modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    if (glow > 0f) {
                        val radius = this.size.minDimension * 1.1f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f * glow),
                                    Color.White.copy(alpha = 0.18f * glow),
                                    Color.Transparent
                                ),
                                center = Offset(this.size.width / 2, this.size.height / 2),
                                radius = radius
                            ),
                            radius = radius
                        )
                    }
                }
                .clip(CircleShape)
                .background(background, CircleShape)
                .border(1.5.dp, borderColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flashlight),
                contentDescription = stringResource(
                    if (isOn) R.string.flashlight_on else R.string.flashlight
                ),
                tint = iconColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/** The button on its own row, used when the clock (time) is not shown. */
@Composable
fun FlashlightRow(
    horizontalAlignment: Alignment.Horizontal,
    isOn: Boolean,
    onClick: () -> Unit,
    contentColor: Color
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        FlashlightButton(isOn = isOn, onClick = onClick, contentColor = contentColor)
    }
}
