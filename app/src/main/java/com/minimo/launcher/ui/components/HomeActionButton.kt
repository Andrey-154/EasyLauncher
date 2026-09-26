package com.minimo.launcher.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R
import com.minimo.launcher.utils.HomeButtonStyle
import com.minimo.launcher.utils.HomeButtonType

private const val ANIMATION_MILLIS = 250

fun HomeButtonType.icon(): ImageVector = when (this) {
    HomeButtonType.FLASHLIGHT -> Icons.Rounded.FlashlightOn
    HomeButtonType.CAMERA -> Icons.Rounded.PhotoCamera
    HomeButtonType.PHONE -> Icons.Rounded.Call
    HomeButtonType.MESSAGES -> Icons.Rounded.Sms
    HomeButtonType.BROWSER -> Icons.Rounded.Public
    HomeButtonType.CALCULATOR -> Icons.Rounded.Calculate
    HomeButtonType.ALARM -> Icons.Rounded.Alarm
    HomeButtonType.WIFI -> Icons.Rounded.Wifi
    HomeButtonType.BLUETOOTH -> Icons.Rounded.Bluetooth
    HomeButtonType.VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
    HomeButtonType.MEDIA_PLAY_PAUSE -> Icons.Rounded.PlayArrow
    HomeButtonType.MEDIA_NEXT -> Icons.Rounded.SkipNext
    HomeButtonType.LOCK_SCREEN -> Icons.Rounded.Lock
    HomeButtonType.NOTIFICATIONS -> Icons.Rounded.Notifications
    HomeButtonType.SEARCH -> Icons.Rounded.Search
    HomeButtonType.SETTINGS -> Icons.Rounded.Settings
    HomeButtonType.APP -> Icons.Rounded.Apps
    HomeButtonType.FOLDER -> Icons.Rounded.Folder
}

@StringRes
fun HomeButtonType.title(): Int = when (this) {
    HomeButtonType.FLASHLIGHT -> R.string.flashlight
    HomeButtonType.CAMERA -> R.string.home_button_camera
    HomeButtonType.PHONE -> R.string.home_button_phone
    HomeButtonType.MESSAGES -> R.string.home_button_messages
    HomeButtonType.BROWSER -> R.string.home_button_browser
    HomeButtonType.CALCULATOR -> R.string.home_button_calculator
    HomeButtonType.ALARM -> R.string.home_button_alarm
    HomeButtonType.WIFI -> R.string.home_button_wifi
    HomeButtonType.BLUETOOTH -> R.string.home_button_bluetooth
    HomeButtonType.VOLUME -> R.string.home_button_volume
    HomeButtonType.MEDIA_PLAY_PAUSE -> R.string.home_button_play_pause
    HomeButtonType.MEDIA_NEXT -> R.string.home_button_next_track
    HomeButtonType.LOCK_SCREEN -> R.string.home_button_lock
    HomeButtonType.NOTIFICATIONS -> R.string.home_button_notifications
    HomeButtonType.SEARCH -> R.string.home_button_search
    HomeButtonType.SETTINGS -> R.string.settings
    HomeButtonType.APP -> R.string.home_button_app
    HomeButtonType.FOLDER -> R.string.folder
}

/**
 * Round quick button. [active] (e.g. flashlight on) fills it white with a soft glow.
 * Shows [appIcon] instead of the vector icon for app buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeActionButton(
    icon: ImageVector,
    contentDescription: String,
    contentColor: Color,
    size: Dp,
    style: HomeButtonStyle,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    appIcon: ImageBitmap? = null,
    /** Drawn instead of [icon] (e.g. a folder preview); gets the icon colour. */
    content: (@Composable (Color) -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val glow by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(ANIMATION_MILLIS),
        label = "buttonGlow"
    )
    val background by animateColorAsState(
        targetValue = when {
            active -> Color.White
            style == HomeButtonStyle.Filled -> contentColor.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        animationSpec = tween(ANIMATION_MILLIS),
        label = "buttonBackground"
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) Color(0xFF1C1C1C) else contentColor.copy(alpha = 0.85f),
        animationSpec = tween(ANIMATION_MILLIS),
        label = "buttonIcon"
    )
    val borderColor = when {
        active -> Color.White
        style == HomeButtonStyle.Filled -> Color.Transparent
        else -> contentColor.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
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
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content(iconColor)
        } else if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.62f)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
