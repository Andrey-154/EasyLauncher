package com.minimo.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.utils.FolderIconStyle
import com.minimo.launcher.utils.HomeButton

/** A closed folder: folder icon, 2×2 preview of its apps, or its first letter in a circle. */
@Composable
fun FolderGlyph(
    folder: HomeButton,
    size: Dp,
    color: Color,
    findApp: (String) -> AppInfo?,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?
) {
    when (folder.iconStyle) {
        FolderIconStyle.Folder -> Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = folder.name,
            tint = color,
            modifier = Modifier.size(size)
        )

        FolderIconStyle.Letter -> Box(
            modifier = Modifier
                .size(size)
                .border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = folder.name.trim().take(1).uppercase().ifEmpty { "•" },
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
            )
        }

        FolderIconStyle.Preview -> {
            val cell = (size - 3.dp) / 2
            val apps = folder.apps.asSequence().mapNotNull(findApp).take(4).toList()
            Column(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.28f))
                    .background(color.copy(alpha = 0.15f))
                    .padding(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (row in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        for (column in 0..1) {
                            val app = apps.getOrNull(row * 2 + column)
                            if (app != null) {
                                MiniAppIcon(app, cell, loadAppIcon)
                            } else {
                                Spacer(modifier = Modifier.size(cell))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAppIcon(app: AppInfo, size: Dp, loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, app.id, sizePx) { value = loadAppIcon(app, sizePx) }
    icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(size)) }
        ?: Spacer(modifier = Modifier.size(size))
}
